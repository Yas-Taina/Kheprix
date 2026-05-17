import re

from guards.base import GuardResult

_LIMITE_PADRAO = 500


def injetar_limit_se_ausente(sql: str, limite: int = _LIMITE_PADRAO) -> str:
    """Injeta LIMIT em queries não-escalares; agregações puras retornam uma linha e não precisam."""
    sql_upper = sql.upper()

    if "LIMIT" in sql_upper:
        return sql

    is_agregacao_escalar = (
        bool(re.search(r"^\s*SELECT\s+(COUNT|SUM|AVG|MIN|MAX)\s*\(", sql, re.IGNORECASE))
        and "GROUP BY" not in sql_upper
    )
    if is_agregacao_escalar:
        return sql

    # em PostgreSQL a ordem correta é ORDER BY ... LIMIT n
    return sql.rstrip().rstrip(";") + f" LIMIT {limite}"


def verificar_sql_output(sql: str) -> GuardResult:
    sql_upper = sql.upper()

    tabelas_sistema = ["PG_CATALOG", "INFORMATION_SCHEMA", "PG_CLASS", "PG_TABLES",
                       "PG_NAMESPACE", "PG_USER", "PG_ROLES", "PG_SHADOW"]
    for tabela in tabelas_sistema:
        if tabela in sql_upper:
            return GuardResult(
                passou=False,
                motivo=f"SQL referencia tabela de sistema não permitida: {tabela}"
            )

    funcoes_proibidas = [r"\bPG_READ_FILE\b", r"\bPG_LS_DIR\b", r"\bPG_SLEEP\b",
                         r"\bCOPY\b", r"\bLO_IMPORT\b", r"\bLO_EXPORT\b"]
    for funcao in funcoes_proibidas:
        if re.search(funcao, sql, re.IGNORECASE):
            return GuardResult(passou=False, motivo="SQL usa função de sistema não permitida.")

    # remove strings literais antes de checar ponto-e-vírgula
    sql_sem_strings = re.sub(r"'[^']*'", "''", sql)
    if ";" in sql_sem_strings.rstrip(";"):
        return GuardResult(passou=False, motivo="SQL contém múltiplos comandos (ponto e vírgula interno).")

    return GuardResult(passou=True)


_PADROES_VAZAMENTO = [
    r"SELECT\s+.{0,100}\s+FROM",
    r"%(estudo_ids)s",
    r"psycopg2",
    r"POSTGRES_DW",
    r"GROQ_API_KEY",
    r"CHATBOT_INTERNAL_KEY",
    r"traceback",
    r"File \".*\.py\"",
    r"\bestudo\s+\d+\b",
    r"\bid[_\s]estudo\s*[=:]\s*\d+",
    r"\bfk[_\s]estudo\s*[=:]\s*\d+",
    r"\bid[_\s]registro\s*[=:]\s*\d+",
]


def verificar_resposta_final(resposta: str) -> GuardResult:
    for padrao in _PADROES_VAZAMENTO:
        if re.search(padrao, resposta, re.IGNORECASE):
            return GuardResult(
                passou=False,
                motivo="Resposta contém informação interna do sistema."
            )
    return GuardResult(passou=True)


def verificar_alucinacao(
    resposta: str,
    dados: list[dict],
    historico: list[dict] | None = None,
) -> GuardResult:
    if not dados:
        frases_validas = [
            "não foram encontrados",
            "nenhum registro",
            "não há registros",
            "sem registros",
            "0 registro",
        ]
        resposta_lower = resposta.lower()
        if not any(frase in resposta_lower for frase in frases_validas):
            return GuardResult(
                passou=False,
                motivo=(
                    "Dados vazios mas resposta não informa ausência de registros. "
                    "Possível alucinação."
                ),
            )
        return GuardResult(passou=True)

    # também aceita len(dados): dizer "X registros" quando X linhas foram retornadas é correto
    valores_reais: set[int] = set()
    if len(dados) > 1:
        valores_reais.add(len(dados))

    tem_coluna_numerica = False
    for linha in dados:
        for valor in linha.values():
            if isinstance(valor, bool) or valor is None:
                continue
            if isinstance(valor, int) and valor > 1:
                valores_reais.add(valor)
                tem_coluna_numerica = True
            elif isinstance(valor, float) and valor == int(valor) and valor > 1:
                valores_reais.add(int(valor))
                tem_coluna_numerica = True

    # resultado puramente textual: sem base numérica para verificar alucinação
    if not tem_coluna_numerica:
        return GuardResult(passou=True)

    # aceita números mencionados em turnos anteriores — evita falso positivo multi-turn
    # ex: "das 3 espécies registradas, 2 são ameaçadas" — o "3" vem do histórico, não é alucinação
    for turno in (historico or []):
        conteudo = turno.get("content", "")
        for n in re.findall(r"\b(\d+)\b", conteudo):
            val = int(n)
            if 1 < val < 10_000:
                valores_reais.add(val)

    numeros_na_resposta = {
        int(n) for n in re.findall(r"\b(\d+)\b", resposta)
        if int(n) > 1
    }

    # anos não indicam alucinação
    anos = {n for n in numeros_na_resposta if 1900 <= n <= 2100}
    numeros_a_verificar = numeros_na_resposta - anos

    numeros_inventados = numeros_a_verificar - valores_reais
    if numeros_inventados:
        return GuardResult(
            passou=False,
            motivo=(
                f"Possível alucinação numérica: número(s) {numeros_inventados} "
                f"citado(s) na resposta não encontrado(s) nos dados reais."
            ),
        )

    return GuardResult(passou=True)
