import logging
from airflow.providers.postgres.hooks.postgres import PostgresHook


def execute_upsert(
    target_table: str,
    primary_keys: list,
    sql_file: str,
    conflict_action: str = "UPDATE",
    postgres_conn_id: str = "db_dw",
    **kwargs,
) -> int:
    """
    Lê um arquivo SQL com uma query SELECT e executa UPSERT na tabela de destino.
    Retorna o rowcount, publicado via XCom para o log_execucao_etl.
    """
    with open(f"/opt/airflow/sql/{sql_file}", "r", encoding="utf-8") as f:
        select_sql = f.read()

    hook = PostgresHook(postgres_conn_id=postgres_conn_id)

    schema, table = ("public", target_table)
    if "." in target_table:
        schema, table = target_table.split(".")

    col_records = hook.get_records(
        f"SELECT column_name FROM information_schema.columns "
        f"WHERE table_schema = '{schema}' AND table_name = '{table}' "
        f"ORDER BY ordinal_position;"
    )
    columns = [r[0] for r in col_records]

    if not columns:
        raise ValueError(f"Tabela {target_table} não encontrada.")

    columns_str = ", ".join(columns)
    pk_str = ", ".join(primary_keys)

    upsert_query = (
        f"INSERT INTO {target_table} ({columns_str})\n"
        f"SELECT * FROM (\n{select_sql}\n) AS temp_selection\n"
        f"ON CONFLICT ({pk_str}) DO "
    )

    if conflict_action.upper() == "UPDATE":
        update_cols = [col for col in columns if col not in primary_keys]
        update_set = ",\n    ".join(f"{col} = EXCLUDED.{col}" for col in update_cols)
        upsert_query += f"UPDATE SET\n    {update_set};"
    elif conflict_action.upper() == "NOTHING":
        upsert_query += "NOTHING;"
    else:
        raise ValueError(f"conflict_action '{conflict_action}' inválido. Use UPDATE ou NOTHING.")

    logging.info(f"UPSERT {target_table}:\n{upsert_query}")

    # hook.run() não expõe rowcount — usa conexão direta
    conn = hook.get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(upsert_query)
            rowcount = cur.rowcount
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    logging.info(f"[{target_table}] {rowcount} linhas processadas.")
    return rowcount
