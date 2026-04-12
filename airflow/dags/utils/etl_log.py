import logging
from airflow.providers.postgres.hooks.postgres import PostgresHook


def registrar_inicio(dag_id: str, run_id: str):
    PostgresHook(postgres_conn_id='db_dw').run("""
        INSERT INTO public.log_execucao_etl (dag_id, run_id, status, iniciado_em)
        VALUES (%s, %s, 'em_andamento', NOW())
        ON CONFLICT (dag_id, run_id) DO NOTHING;
    """, parameters=(dag_id, run_id))
    logging.info(f"[ETL LOG] Início — run_id: {run_id}")


def registrar_conclusao(dag_id: str, run_id: str, task_ids: list, ti):
    total_rows = sum(
        ti.xcom_pull(task_ids=task_id) or 0
        for task_id in task_ids
        if isinstance(ti.xcom_pull(task_ids=task_id), int)
    )
    PostgresHook(postgres_conn_id='db_dw').run("""
        UPDATE public.log_execucao_etl
        SET status = 'concluido', concluido_em = NOW(), registros_processados = %s
        WHERE dag_id = %s AND run_id = %s;
    """, parameters=(total_rows, dag_id, run_id))
    logging.info(f"[ETL LOG] Concluído — {total_rows} registros — run_id: {run_id}")


def registrar_falha(dag_id: str, context):
    run_id = context['run_id']
    task_id = context['task_instance'].task_id
    PostgresHook(postgres_conn_id='db_dw').run("""
        UPDATE public.log_execucao_etl
        SET status = 'falhou', concluido_em = NOW(), detalhes = %s
        WHERE dag_id = %s AND run_id = %s;
    """, parameters=(f"Falha na task: {task_id}", dag_id, run_id))
    logging.error(f"[ETL LOG] Falha — task: {task_id} — run_id: {run_id}")
