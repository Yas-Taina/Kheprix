import re

_OPERACOES_PROIBIDAS = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|GRANT|REVOKE|EXEC|EXECUTE|COPY|VACUUM|ANALYZE)\b",
    re.IGNORECASE,
)

_TABELAS_PERMITIDAS = {"indicadores_dashboard", "analises_estatisticas"}
_FILTRO_MULTI_TENANT = "%(estudo_ids)s"


def validar_sql(sql: str) -> None:
    if not sql or not sql.strip():
        raise ValueError("SQL vazio recebido do modelo.")

    sql_stripped = sql.strip()

    if not sql_stripped.upper().startswith("SELECT"):
        raise ValueError("Apenas queries SELECT são permitidas.")

    if _OPERACOES_PROIBIDAS.search(sql_stripped):
        raise ValueError("SQL contém operação não permitida (DDL ou DML).")

    if _FILTRO_MULTI_TENANT not in sql_stripped:
        raise ValueError(
            "SQL sem filtro de autorização multi-tenant. "
            "O parâmetro %(estudo_ids)s é obrigatório."
        )

    tabelas_encontradas = set(
        t
        for par in re.findall(
            r"(?:FROM|JOIN)\s+(?:public\.)?(\w+)", sql_stripped, re.IGNORECASE
        )
        for t in [par]
        if t
    )
    tabelas_nao_permitidas = tabelas_encontradas - _TABELAS_PERMITIDAS
    if tabelas_nao_permitidas:
        raise ValueError(
            f"SQL referencia tabelas não autorizadas: {tabelas_nao_permitidas}. "
            f"Permitidas: {_TABELAS_PERMITIDAS}."
        )
