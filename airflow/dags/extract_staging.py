from datetime import datetime, timedelta, timezone
from airflow import DAG
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.operators.python import PythonOperator, ShortCircuitOperator
from airflow.operators.trigger_dagrun import TriggerDagRunOperator
import tempfile
import logging

default_args = {
    'owner': 'admin',
    'depends_on_past': False,
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 0,
}

TABLES_TO_EXTRACT = {
    'usuarios': 'full',
    'estudos': 'full',
    'colaboradores': 'full',
    'campanhas': 'full',
    'especies': 'full',
    'unidades_amostrais': 'full',
    'eventos_amostragem': 'incremental',
    'registro_ocorrencias': 'incremental',
    'variaveis': 'full',
    'valores_variaveis': 'incremental'
}

# colaboradores excluída: join table sem updated_at rastreável no OLTP
TABELAS_MONITORADAS_OLTP = [
    'estudos', 'campanhas', 'especies', 'unidades_amostrais',
    'eventos_amostragem', 'registro_ocorrencias', 'variaveis', 'valores_variaveis',
]


def _verificar_dados_novos(**kwargs) -> bool:
    """Retorna False (skip) se nenhuma tabela OLTP tem updated_at posterior ao start_date do último run bem-sucedido."""
    from airflow.models import DagRun
    from airflow.utils.state import DagRunState

    runs_concluidos = DagRun.find(dag_id='extract_staging', state=DagRunState.SUCCESS)

    if not runs_concluidos:
        logging.info("[SHORT CIRCUIT] Carga inicial — processando.")
        return True

    ultimo_run = max(runs_concluidos, key=lambda r: r.start_date)
    ultima_extracao = ultimo_run.start_date
    logging.info(f"[SHORT CIRCUIT] Última extração iniciou em: {ultima_extracao}")

    source_hook = PostgresHook(postgres_conn_id='db_oltp')
    for tabela in TABELAS_MONITORADAS_OLTP:
        res = source_hook.get_first(f"SELECT MAX(updated_at) FROM {tabela};")
        max_updated = res[0] if res else None
        if max_updated:
            if max_updated.tzinfo is None:
                max_updated = max_updated.replace(tzinfo=timezone.utc)
            if max_updated > ultima_extracao:
                logging.info(f"[SHORT CIRCUIT] '{tabela}' tem dados novos. Processando.")
                return True

    logging.info("[SHORT CIRCUIT] Sem dados novos. Pulando extração.")
    return False


def extract_and_load_table(table_name, load_strategy, **kwargs):
    logging.info(f"Extraindo {table_name} ({load_strategy.upper()})...")
    source_hook = PostgresHook(postgres_conn_id='db_oltp')
    dest_hook = PostgresHook(postgres_conn_id='db_dw')

    res = dest_hook.get_first(f"SELECT COUNT(*) FROM staging.{table_name}")
    dw_count = res[0] if res else 0
    actual_strategy = 'full' if dw_count == 0 else load_strategy

    if actual_strategy == 'full' and load_strategy == 'incremental':
        logging.info(f"{table_name}: staging vazio — full load inicial.")

    with tempfile.NamedTemporaryFile(suffix='.csv') as tmp_file:
        sql_export = f"COPY {table_name} TO STDOUT WITH CSV HEADER;"

        if actual_strategy == 'incremental':
            res_max = dest_hook.get_first(f"SELECT MAX(updated_at) FROM staging.{table_name};")
            max_updated_at = res_max[0] if res_max and res_max[0] else None

            if max_updated_at:
                # Overlap de 10 min protege contra late-arriving data; o UPSERT garante idempotência.
                hwm_com_overlap = max_updated_at - timedelta(minutes=10)
                sql_export = f"COPY (SELECT * FROM {table_name} WHERE updated_at >= '{hwm_com_overlap.isoformat()}') TO STDOUT WITH CSV HEADER;"
                logging.info(f"{table_name}: HWM={max_updated_at}, capturando a partir de {hwm_com_overlap}")

        source_hook.copy_expert(sql_export, tmp_file.name)

        with open(tmp_file.name, 'r', encoding='utf-8') as f:
            lines = sum(1 for line in f)

        if lines <= 1 and actual_strategy == 'incremental':
            logging.info(f"{table_name}: sem dados novos.")
            return

        logging.info(f"{table_name}: {lines-1} registros para carregar.")

        col_records = source_hook.get_records(
            f"SELECT column_name FROM information_schema.columns "
            f"WHERE table_name = '{table_name}' ORDER BY ordinal_position;"
        )
        col_list = [r[0] for r in col_records]
        cols_sql = "(" + ", ".join(col_list) + ")"

        if actual_strategy == 'full':
            dest_hook.run(f"TRUNCATE TABLE staging.{table_name} CASCADE;")
            dest_hook.copy_expert(f"COPY staging.{table_name} {cols_sql} FROM STDIN WITH CSV HEADER;", tmp_file.name)
        else:
            # TEMP TABLE na mesma conexão: escopo da sessão, sem conflito com runs paralelos
            temp_table = f"tmp_{table_name}"
            conn = dest_hook.get_conn()
            try:
                cur = conn.cursor()
                cur.execute(f"CREATE TEMP TABLE {temp_table} AS SELECT * FROM staging.{table_name} LIMIT 0;")
                with open(tmp_file.name, 'r', encoding='utf-8') as f_csv:
                    cur.copy_expert(f"COPY {temp_table} {cols_sql} FROM STDIN WITH CSV HEADER;", f_csv)
                update_set = ", ".join([f"{col} = EXCLUDED.{col}" for col in col_list if col != 'id'])
                cur.execute(f"""
                    INSERT INTO staging.{table_name} {cols_sql}
                    SELECT {', '.join(col_list)} FROM {temp_table}
                    ON CONFLICT (id) DO UPDATE SET {update_set};
                """)
                cur.execute(f"DROP TABLE {temp_table};")
                conn.commit()
            except Exception as e:
                conn.rollback()
                raise e
            finally:
                cur.close()
                conn.close()

        logging.info(f"{table_name}: concluído.")


with DAG(
    'extract_staging',
    default_args=default_args,
    description='Extração OLTP → Staging (micro-batch 5min)',
    schedule='*/5 * * * *',
    start_date=datetime(2025, 1, 1),
    catchup=False,
    tags=['kheprix', 'etl'],
) as dag:

    task_verificar_novos = ShortCircuitOperator(
        task_id='verificar_dados_novos',
        python_callable=_verificar_dados_novos,
    )

    extract_tasks = {}
    for table, strategy in TABLES_TO_EXTRACT.items():
        extract_tasks[table] = PythonOperator(
            task_id=f'extract_{table}',
            python_callable=extract_and_load_table,
            op_kwargs={'table_name': table, 'load_strategy': strategy},
        )

    task_trigger_transform = TriggerDagRunOperator(
        task_id='trigger_transform_star_schema',
        trigger_dag_id='transform_star_schema',
        wait_for_completion=False,
        trigger_rule='all_done',  # dispara mesmo quando extrações foram skippadas
    )

    task_verificar_novos >> list(extract_tasks.values())
    for table_task in extract_tasks.values():
        table_task >> task_trigger_transform
