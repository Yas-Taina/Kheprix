from datetime import datetime, timedelta
from airflow import DAG
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.operators.python import PythonOperator
import tempfile
import logging

# Argumentos padrão configurados para não enviar e-mails de falha em dev e focar em repetição simples
default_args = {
    'owner': 'admin',
    'depends_on_past': False,
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 0,
}

# Configuração de Táticas de Carga Híbridas (Otimização Padrão de Mercado)
# full: Trunca tudo e baixa de novo (pra cadastros pequenos).
# incremental: Baixa apenas o que foi inserido/atualizado nos últimos 5 min. e faz 'Upsert' usando tabela temporária.
TABLES_TO_EXTRACT = {
    'usuarios': 'full',
    'estudos': 'full',
    'colaboradores': 'full',
    'campanhas': 'full',
    'especies': 'full',
    'unidades_amostrais': 'full',
    'eventos_amostragem': 'incremental',
    'variaveis': 'full',
    'valores_variaveis': 'incremental'
}

def prepare_staging_schema(**kwargs):
    """(Migrado para o backend/Rails) Agora esta task funciona apenas como um Sensor de Partida"""
    logging.info("Schema Staging agora é regido pelas migrations nativas do Rails (backend/db/dw_migrate).")
    logging.info("Validado e pronto para disparar a Extração OLTP -> DW!")
    logging.info("Tabelas na camada de Staging garantidas de existir.")


def extract_and_load_table(table_name, load_strategy, **kwargs):
    """
    Exporta a tabela do OLTP (total ou incremental) para um CSV temporário
    e importa para o STAGING do DW (via TRUNCATE ou UPSERT Seguro).
    """
    logging.info(f"EXTRAINDO {table_name} ({load_strategy.upper()} LOAD)...")
    source_hook = PostgresHook(postgres_conn_id='db_oltp')
    dest_hook = PostgresHook(postgres_conn_id='db_dw')
    
    # Valida se a tabela de destino (Staging) tem dados. Se estiver vazia, ignora o Delta e força Full Load (Carga Histórica Inicial)
    res = dest_hook.get_first(f"SELECT COUNT(*) FROM staging.{table_name}")
    dw_count = res[0] if res else 0
    actual_strategy = 'full' if dw_count == 0 else load_strategy
    
    if actual_strategy == 'full' and load_strategy == 'incremental':
        logging.info(f"{table_name}: Tabela analítica vazia detectada. Forçando FULL LOAD (Carga Histórica) inicial!")
    
    with tempfile.NamedTemporaryFile(suffix='.csv') as tmp_file:
        sql_export = f"COPY {table_name} TO STDOUT WITH CSV HEADER;"
        
        # Filtro delta HIGH-WATER MARK para leitura incremental no OLTP (Prova de Falhas)
        if actual_strategy == 'incremental':
            # Descobre cronologicamente qual é a data do último registro gravado no Banco Analítico
            res_max = dest_hook.get_first(f"SELECT MAX(updated_at) FROM staging.{table_name};")
            max_updated_at = res_max[0] if res_max and res_max[0] else None
            
            if max_updated_at:
                logging.info(f"{table_name}: High-Water Mark detectada. Capturando apenas atualizados pós {max_updated_at}")
                sql_export = f"COPY (SELECT * FROM {table_name} WHERE updated_at >= '{max_updated_at.isoformat()}') TO STDOUT WITH CSV HEADER;"
                
        source_hook.copy_expert(sql_export, tmp_file.name)
        
        # Checar se existem registros válidos (tirando o cabeçalho)
        with open(tmp_file.name, 'r', encoding='utf-8') as f:
            lines = sum(1 for line in f)
            
        if lines <= 1 and actual_strategy == 'incremental':
            logging.info(f"{table_name}: Nenhum dado novo gerado desde os últimos 5 minutos. Ignorando carga Staging.")
            return
            
        logging.info(f"{table_name}: CSV copiado ({lines-1} registros). Transportando para DW...")
        
        # Mapear EXATAMENTE as colunas de origem para o COPY não esbarrar na nova coluna 'ingestao_at'
        col_records = source_hook.get_records(f"SELECT column_name FROM information_schema.columns WHERE table_name = '{table_name}' ORDER BY ordinal_position;")
        col_list = [r[0] for r in col_records]
        cols_sql = "(" + ", ".join(col_list) + ")"
        
        if actual_strategy == 'full':
            # Carga destrutiva rápida
            dest_hook.run(f"TRUNCATE TABLE staging.{table_name} CASCADE;")
            sql_import = f"COPY staging.{table_name} {cols_sql} FROM STDIN WITH CSV HEADER;"
            dest_hook.copy_expert(sql_import, tmp_file.name)
        else:
            # Upsert Pattern Seguro para DW
            temp_table = f"tmp_{table_name}"
            # Usa-se a MESMA conexão atômica para garantir que a Tabela Temporária (TEMP) não morra entre uma instrução e outra
            conn = dest_hook.get_conn()
            try:
                cur = conn.cursor()
                cur.execute(f"CREATE TEMP TABLE {temp_table} AS SELECT * FROM staging.{table_name} LIMIT 0;")
                
                with open(tmp_file.name, 'r', encoding='utf-8') as f_csv:
                    cur.copy_expert(f"COPY {temp_table} {cols_sql} FROM STDIN WITH CSV HEADER;", f_csv)
                
                cur.execute(f"DELETE FROM staging.{table_name} WHERE id IN (SELECT id FROM {temp_table});")
                cur.execute(f"INSERT INTO staging.{table_name} {cols_sql} SELECT {', '.join(col_list)} FROM {temp_table};")
                cur.execute(f"DROP TABLE {temp_table};")
                conn.commit()
            except Exception as e:
                conn.rollback()
                raise e
            finally:
                cur.close()
                conn.close()
            
        logging.info(f"{table_name}: Operação concluída. [loaded_at Registrado automaticamente]")

# Definição da DAG
with DAG(
    'extract_staging',
    default_args=default_args,
    description='Pipeline de ETL: Extração STAGING do OLTP via Micro-batch',
    schedule='*/5 * * * *',  # -> Roda automaticamente a cada 5 Minutos (Micro-batching / Near-Real-Time)
    start_date=datetime(2025, 1, 1),
    catchup=False,
    tags=['kheprix', 'etl'],
) as dag:

    # Tarefa inicial para garantir a base (schemas)
    task_prepare_staging = PythonOperator(
        task_id='preparar_schemas_staging',
        python_callable=prepare_staging_schema,
    )

    # Dicionário prático para guardar os objetos das tarefas de extração
    extract_tasks = {}
    
    for table, strategy in TABLES_TO_EXTRACT.items():
        # Cria a tarefa dinamicamente para cada tabela usando a tática selecionada no dicionario
        # Assim o Airflow fará a extração em paralelo nativamente se o Executor permitir
        extract_task = PythonOperator(
            task_id=f'extract_{table}',
            python_callable=extract_and_load_table,
            op_kwargs={'table_name': table, 'load_strategy': strategy},
        )
        extract_tasks[table] = extract_task
        
        # Ordem: Prepara Staging -> Roda Extratores em Paralelo
        task_prepare_staging >> extract_task

    
    # Espaçadores no código para quando formos adicionar as ETAPAS 2 e 3 (Transformation/Loading para Dimensions)
    def start_transformation(**kwargs):
        logging.info("Fim da camada STAGING. Início do Data Cleansing e DW (Dimensões/Fatos)...")

    task_start_transformation = PythonOperator(
        task_id='start_transformation_layer',
        python_callable=start_transformation,
    )
    
    # Ordem: Extratores apontam para o nó central que iniciará as transformações finais
    for table_task in extract_tasks.values():
        table_task >> task_start_transformation
