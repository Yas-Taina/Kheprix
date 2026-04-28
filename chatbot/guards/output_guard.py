"""
Guard Rails de Saída
====================
Valida e sanitiza as saídas do modelo:
  1. SQL gerado        — antes de executar no DW
  2. Resposta final    — antes de enviar ao usuário
  3. Anti-alucinação  — verifica que números citados existem nos dados reais
"""
import re

from guards.base import GuardResult

_LIMITE_PADRAO = 500


# ---------------------------------------------------------------------------
# Guard Rails do SQL gerado
# ---------------------------------------------------------------------------

def injetar_limit_se_ausente(sql: str, limite: int = _LIMITE_PADRAO) -> str:
    """
    Garante que toda query não-escalar tenha LIMIT.
    Queries de agregação pura (ex: SELECT COUNT(*) FROM ...) retornam
    uma única linha e não precisam de LIMIT.
    """
    sql_upper = sql.upper()

    if "LIMIT" in sql_upper:
        return sql

    # Agregação escalar sem GROUP BY → retorna uma única linha, sem LIMIT necessário
    is_agregacao_escalar = (
        bool(re.search(r"^\s*SELECT\s+(COUNT|SUM|AVG|MIN|MAX)\s*\(", sql, re.IGNORECASE))
        and "GROUP BY" not in sql_upper
    )
    if is_agregacao_escalar:
        return sql

    # Para todos os outros casos, injeta LIMIT no final (após ORDER BY se houver).
    # Em PostgreSQL a ordem correta é: ORDER BY ... LIMIT n
    return sql.rstrip().rstrip(";") + f" LIMIT {limite}"


def verificar_sql_output(sql: str) -> GuardResult:
    """
    Segunda camada de verificação do SQL (além do sql_validator).
    Detecta padrões de exfiltração que podem escapar da validação primária.
    """
    sql_upper = sql.upper()

    # Subqueries em tabelas fora do escopo do DW (ex: pg_catalog, information_schema)
    tabelas_sistema = ["PG_CATALOG", "INFORMATION_SCHEMA", "PG_CLASS", "PG_TABLES",
                       "PG_NAMESPACE", "PG_USER", "PG_ROLES", "PG_SHADOW"]
    for tabela in tabelas_sistema:
        if tabela in sql_upper:
            return GuardResult(
                passou=False,
                motivo=f"SQL referencia tabela de sistema não permitida: {tabela}"
            )

    # Funções que podem exfiltrar dados do servidor
    funcoes_proibidas = [r"\bPG_READ_FILE\b", r"\bPG_LS_DIR\b", r"\bPG_SLEEP\b",
                         r"\bCOPY\b", r"\bLO_IMPORT\b", r"\bLO_EXPORT\b"]
    for funcao in funcoes_proibidas:
        if re.search(funcao, sql, re.IGNORECASE):
            return GuardResult(passou=False, motivo="SQL usa função de sistema não permitida.")

    # Detect stacked queries (tentativa de executar múltiplos comandos)
    # Remove strings literais antes de checar ponto-e-vírgula
    sql_sem_strings = re.sub(r"'[^']*'", "''", sql)
    if ";" in sql_sem_strings.rstrip(";"):
        return GuardResult(passou=False, motivo="SQL contém múltiplos comandos (ponto e vírgula interno).")

    return GuardResult(passou=True)


# ---------------------------------------------------------------------------
# Guard Rails da resposta final em linguagem natural
# ---------------------------------------------------------------------------

# Padrões que indicam que o modelo vazou informação interna na resposta
_PADROES_VAZAMENTO = [
    r"SELECT\s+.{0,100}\s+FROM",       # SQL na resposta
    r"%(estudo_ids)s",                  # placeholder interno exposto
    r"psycopg2",                        # library de banco
    r"POSTGRES_DW",                     # variável de ambiente
    r"GROQ_API_KEY",                    # chave de API
    r"CHATBOT_INTERNAL_KEY",            # chave interna
    r"traceback",                       # stack trace Python
    r"File \".*\.py\"",                 # path de arquivo interno
    # IDs de backend que nunca devem aparecer na resposta ao usuário
    r"\bestudo\s+\d+\b",               # "estudo 16", "estudo 42"
    r"\bid[_\s]estudo\s*[=:]\s*\d+",  # "id_estudo: 16"
    r"\bfk[_\s]estudo\s*[=:]\s*\d+",  # "fk_estudo = 16"
    r"\bid[_\s]registro\s*[=:]\s*\d+", # "id_registro: 5"
]


def verificar_resposta_final(resposta: str) -> GuardResult:
    """
    Verifica se a resposta final ao usuário não contém vazamento de
    informações internas do sistema.
    """
    for padrao in _PADROES_VAZAMENTO:
        if re.search(padrao, resposta, re.IGNORECASE):
            return GuardResult(
                passou=False,
                motivo="Resposta contém informação interna do sistema."
            )
    return GuardResult(passou=True)


# ---------------------------------------------------------------------------
# Guard Rail de Anti-alucinação
# ---------------------------------------------------------------------------

def verificar_alucinacao(
    resposta: str,
    dados: list[dict],
    historico: list[dict] | None = None,
) -> GuardResult:
    """
    Verifica se os números inteiros citados na resposta existem nos dados reais.

    Estratégia: extrai todos os inteiros > 1 da resposta e verifica se cada
    um aparece como valor em pelo menos uma célula dos dados retornados pelo DW,
    ou como número citado em turnos anteriores da sessão (historico).

    Isso evita falsos positivos multi-turn: quando o modelo diz "das 3 espécies
    registradas, 2 são ameaçadas", o "3" vem do contexto anterior — não é alucinação.

    Limitação intencional: apenas verifica inteiros (não floats) para evitar
    falsos positivos com anos, IDs e valores que o modelo formata diferente.
    """
    if not dados:
        # Se não há dados, a única resposta válida é "não encontrado"
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

    # Coleta todos os valores numéricos inteiros presentes nos dados reais.
    # Também aceita len(dados): dizer "X registros" quando X linhas foram retornadas é correto.
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

    # Resultado puramente textual (só strings/booleans): o guard não tem base numérica
    # para verificar alucinação. Números na resposta são referências de contexto, não dados.
    # Ex: "Qual o nome das espécies?" retorna só nome_cientifico — não há como alucinar valores.
    if not tem_coluna_numerica:
        return GuardResult(passou=True)

    # Aceita números já mencionados em turnos anteriores da sessão.
    # "Das 3 espécies totais, 2 são ameaçadas" — o "3" vem do histórico, não é alucinação.
    for turno in (historico or []):
        conteudo = turno.get("content", "")
        for n in re.findall(r"\b(\d+)\b", conteudo):
            val = int(n)
            if 1 < val < 10_000:   # ignora anos e IDs muito grandes
                valores_reais.add(val)

    # Extrai números inteiros citados na resposta (ignora 0 e 1 — muito comuns)
    numeros_na_resposta = {
        int(n) for n in re.findall(r"\b(\d+)\b", resposta)
        if int(n) > 1
    }

    # Anos são esperados e não indicam alucinação (ex: "2024", "2023")
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
