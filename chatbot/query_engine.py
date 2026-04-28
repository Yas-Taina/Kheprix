"""
Pipeline Text-to-SQL
====================
Orquestra as 3 etapas de uma consulta:
  1. Geração de SQL (Groq/Llama)      ← guard rail de entrada + output guard no SQL
  2. Execução no DW                   ← sql_validator + filtro multi-tenant paramétrico
  3. Interpretação do resultado       ← output guard de alucinação + vazamento

Multi-turn: recebe histórico de sessão e passa ao LLM para resolver referências
pronominais ("e dessas?", "nessas espécies") entre perguntas.

Nenhum dado do usuário é interpolado diretamente em strings SQL.
Os estudo_ids são sempre passados como parâmetro psycopg2.
A conexão com o DW é read-only (configurado no pool em db.py).
"""
import json
import logging
import re
import time
import psycopg2
import psycopg2.extras
from openai import OpenAI, APIStatusError

from config import GROQ_API_KEY, GROQ_MODEL
from db import obter_conexao
from schema_context import SYSTEM_PROMPT, INTERPRETACAO_PROMPT
from guards.sql_validator import validar_sql
from guards.input_guard import validar_entrada
from guards.output_guard import (
    injetar_limit_se_ausente,
    verificar_sql_output,
    verificar_resposta_final,
    verificar_alucinacao,
)

logger = logging.getLogger("kheprix.chatbot")

# Groq usa API OpenAI-compatible
_client = OpenAI(
    api_key=GROQ_API_KEY,
    base_url="https://api.groq.com/openai/v1",
)

# Códigos HTTP que indicam erro transiente — vale fazer retry
_ERROS_TRANSIENTES = {429, 500, 502, 503, 504}
_MAX_RETRIES = 2
_RETRY_DELAY_S = 4


# ---------------------------------------------------------------------------
# Helper de retry para chamadas ao LLM
# ---------------------------------------------------------------------------

def _chamar_llm_com_retry(**kwargs) -> object:
    """
    Chama _client.chat.completions.create com retry automático para erros
    transientes (429/503). Lança exceção após _MAX_RETRIES tentativas.
    """
    for tentativa in range(_MAX_RETRIES + 1):
        try:
            return _client.chat.completions.create(**kwargs)
        except APIStatusError as exc:
            if exc.status_code in _ERROS_TRANSIENTES and tentativa < _MAX_RETRIES:
                logger.warning(
                    "llm_erro_transiente_retry",
                    extra={"tentativa": tentativa + 1, "status": exc.status_code},
                )
                time.sleep(_RETRY_DELAY_S * (tentativa + 1))
                continue
            raise


# ---------------------------------------------------------------------------
# Etapa 1 — Geração de SQL
# ---------------------------------------------------------------------------

def _gerar_sql(pergunta: str, historico: list[dict]) -> dict:
    """
    Chama o Groq (Llama 3.3 70B) para converter a pergunta em SQL estruturado.

    O histórico da sessão é passado como mensagens anteriores para que o modelo
    resolva referências pronominais multi-turn ("e dessas?", "nessas espécies").
    """
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    messages.extend(historico)
    messages.append({"role": "user", "content": pergunta})

    resposta = _chamar_llm_com_retry(
        model=GROQ_MODEL,
        messages=messages,
        response_format={"type": "json_object"},
        temperature=0.1,
        max_tokens=1024,
    )
    conteudo = resposta.choices[0].message.content
    try:
        return json.loads(conteudo)
    except json.JSONDecodeError:
        raise ValueError(
            f"Modelo retornou resposta não-JSON. Conteúdo: {conteudo[:200]}"
        )


# ---------------------------------------------------------------------------
# Etapa 2 — Execução no DW
# ---------------------------------------------------------------------------

def _executar_sql(sql: str, estudo_ids: list[int]) -> tuple[list[str], list[dict]]:
    """
    Valida e executa o SQL no DW via connection pool.

    Os estudo_ids NUNCA são interpolados na string SQL — sempre passados como
    parâmetro psycopg2 (proteção contra SQL injection).

    A conexão já é read-only por configuração do pool (db.py), tornando
    fisicamente impossível qualquer escrita mesmo que um SQL inválido
    escape dos guards.
    """
    # Guard rail primário: estrutura, multi-tenant, tabelas permitidas
    validar_sql(sql)

    # Guard rail secundário: tabelas de sistema, funções proibidas, stacked queries
    resultado_guard = verificar_sql_output(sql)
    if not resultado_guard.passou:
        raise ValueError(f"Guard rail SQL: {resultado_guard.motivo}")

    # Injeta LIMIT automaticamente se ausente
    sql = injetar_limit_se_ausente(sql)

    with obter_conexao() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, {"estudo_ids": estudo_ids})
            colunas = [desc.name for desc in cur.description] if cur.description else []
            linhas = [dict(row) for row in cur.fetchall()]

    return colunas, linhas


# ---------------------------------------------------------------------------
# Auxiliares de interpretação e pós-processamento de SQL
# ---------------------------------------------------------------------------

def _resposta_contradiz_dados(resposta: str, dados: list[dict]) -> bool:
    """
    Detecta quando o modelo afirma que não há dados, mas os dados têm valores.
    """
    frases_sem_dados = ["não foram encontrados", "nenhum registro", "sem registros", "nenhuma espécie"]
    if not any(f in resposta.lower() for f in frases_sem_dados):
        return False
    return any(
        isinstance(v, (int, float)) and v != 0
        for row in dados for v in row.values()
    )


def _gerar_resposta_direta(dados: list[dict]) -> str:
    """Fallback determinístico quando o modelo contradiz os dados."""
    if len(dados) == 1:
        partes = [f"{k.replace('_', ' ')}: {v}" for k, v in dados[0].items()]
        return "Resultado da consulta — " + ", ".join(partes) + "."
    return f"A consulta retornou {len(dados)} registro(s) com os dados disponíveis na tabela."


def _corrigir_aspas_sql(sql: str) -> str:
    """Converte aspas duplas em aspas simples para literais string em PostgreSQL."""
    return re.sub(
        r'((?:=|ILIKE|LIKE|IN|<>|!=)\s*)"([^"]*)"',
        r"\1'\2'",
        sql,
        flags=re.IGNORECASE,
    )


def _escapar_percent_sql(sql: str) -> str:
    """
    Dobra % em padrões ILIKE/LIKE para evitar conflito com parâmetros psycopg2.
    ILIKE '%B0%' → ILIKE '%%B0%%' (psycopg2 interpreta % como início de placeholder).
    """
    def _dobrar(match: re.Match) -> str:
        return f"{match.group(1)} '{match.group(2).replace('%', '%%')}'"

    return re.sub(r"(ILIKE|LIKE)\s+'([^']*)'", _dobrar, sql, flags=re.IGNORECASE)


def _enriquecer_sql_especies(sql: str) -> str:
    """
    Pós-processamento determinístico do SQL gerado pelo modelo.

    Com o Groq (70B) isso raramente é necessário, mas mantido como fallback:
      1. COUNT(DISTINCT nome_cientifico) sem STRING_AGG → injeta STRING_AGG
      2. SELECT nome_cientifico sem DISTINCT → adiciona DISTINCT + ORDER BY
    Exceção: queries com is_ameacada preservam SELECT DISTINCT completo.
    """
    tem_filtro_ameaca = bool(re.search(r"is_ameacada", sql, re.IGNORECASE))

    # Caso 1: COUNT sem STRING_AGG (e sem filtro de ameaça)
    if (
        re.search(r"COUNT\s*\(\s*DISTINCT\s+nome_cientifico\s*\)", sql, re.IGNORECASE)
        and not re.search(r"STRING_AGG", sql, re.IGNORECASE)
        and not tem_filtro_ameaca
    ):
        sql = re.sub(
            r"(COUNT\s*\(\s*DISTINCT\s+nome_cientifico\s*\)\s+AS\s+\w+)",
            r"\1, STRING_AGG(DISTINCT nome_cientifico, ', ' ORDER BY nome_cientifico)"
            r" AS especies_registradas",
            sql,
            flags=re.IGNORECASE,
        )

    # Caso 2: SELECT nome_cientifico sem DISTINCT
    if re.search(r"SELECT\s+nome_cientifico\b", sql, re.IGNORECASE):
        if not re.search(r"SELECT\s+DISTINCT", sql, re.IGNORECASE):
            sql = re.sub(
                r"SELECT\s+nome_cientifico",
                "SELECT DISTINCT nome_cientifico",
                sql,
                flags=re.IGNORECASE,
            )
            if not re.search(r"\bORDER\s+BY\b", sql, re.IGNORECASE):
                sql = sql.rstrip().rstrip(";") + " ORDER BY nome_cientifico"

    return sql


# ---------------------------------------------------------------------------
# Etapa 3 — Interpretação em linguagem natural
# ---------------------------------------------------------------------------

def _interpretar_resultado(
    pergunta: str,
    colunas: list[str],
    dados: list[dict],
    sql: str,
    historico: list[dict],
) -> str:
    """
    Chama o Groq (Llama 3.3 70B) para traduzir os dados em resposta em português.

    O histórico é passado para que respostas de follow-up sejam coerentes
    com o que foi respondido anteriormente ("e dessas, quais são ameaçadas?").
    """
    amostra = dados[:20]

    conteudo_usuario = (
        f'Pergunta do pesquisador: "{pergunta}"\n\n'
        f"SQL executado: {sql}\n"
        f"Colunas retornadas: {colunas}\n"
        f"Total de registros encontrados: {len(dados)}\n"
        f"Amostra dos dados (até 20 linhas):\n"
        f"{json.dumps(amostra, default=str, ensure_ascii=False)}"
    )

    messages = [{"role": "system", "content": INTERPRETACAO_PROMPT}]
    messages.extend(historico)
    messages.append({"role": "user", "content": conteudo_usuario})

    resposta = _chamar_llm_com_retry(
        model=GROQ_MODEL,
        messages=messages,
        temperature=0.1,
        max_tokens=600,
    )
    return resposta.choices[0].message.content


# ---------------------------------------------------------------------------
# Pipeline principal
# ---------------------------------------------------------------------------

def processar_pergunta(
    pergunta: str,
    estudo_ids: list[int],
    usuario_id: int,
    historico: list[dict] | None = None,
) -> dict:
    """
    Pipeline completo com guard rails em cada etapa.

    Args:
        pergunta:    Pergunta em linguagem natural do pesquisador.
        estudo_ids:  IDs dos estudos autorizados para o usuário (multi-tenant).
        usuario_id:  ID do usuário autenticado (para logging e rate limiting).
        historico:   Mensagens anteriores da sessão (multi-turn context).

    Retorna dict com:
      resposta  str        resposta em linguagem natural para o usuário
      dados     list[dict] registros brutos do DW (para o frontend)
      sql       str|None   query executada (transparência / modo debug)
      total     int        número de registros retornados
      erro      str|None   mensagem de erro interna (não exposta ao usuário)
    """
    inicio = time.monotonic()
    historico = historico or []

    # ------------------------------------------------------------------
    # Guard Rail de Entrada — antes de qualquer chamada ao modelo
    # ------------------------------------------------------------------
    guard_entrada = validar_entrada(pergunta)
    if not guard_entrada.passou:
        logger.warning(
            "guard_entrada_bloqueou",
            extra={"usuario_id": usuario_id, "motivo": guard_entrada.motivo},
        )
        return {
            "resposta": guard_entrada.motivo,
            "dados": [],
            "sql": None,
            "total": 0,
            "erro": None,
        }

    # ------------------------------------------------------------------
    # Etapa 1: Geração de SQL (com histórico para multi-turn)
    # ------------------------------------------------------------------
    try:
        resultado_modelo = _gerar_sql(pergunta, historico)
    except Exception as exc:
        logger.error("falha_gerar_sql", extra={"usuario_id": usuario_id, "erro": str(exc)})
        return {
            "resposta": "Não consegui entender a pergunta. Tente reformulá-la.",
            "dados": [],
            "sql": None,
            "total": 0,
            "erro": f"Falha ao gerar SQL: {exc}",
        }

    sql = resultado_modelo.get("sql")
    explicacao = resultado_modelo.get("explicacao", "")

    if not sql:
        logger.info(
            "modelo_nao_pode_responder",
            extra={"usuario_id": usuario_id, "explicacao": explicacao},
        )
        return {"resposta": explicacao, "dados": [], "sql": None, "total": 0, "erro": None}

    # Pós-processamento: corrige padrões comuns de geração de SQL
    sql = _corrigir_aspas_sql(sql)
    sql = _escapar_percent_sql(sql)
    sql = _enriquecer_sql_especies(sql)

    # ------------------------------------------------------------------
    # Etapa 2: Validação + Execução no DW
    # ------------------------------------------------------------------
    try:
        colunas, dados = _executar_sql(sql, estudo_ids)
        # SUM/AVG de conjunto vazio retorna NULL no PostgreSQL → converte para 0
        if len(dados) == 1 and all(v is None for v in dados[0].values()):
            dados = [{k: 0 for k in dados[0]}]
    except ValueError as exc:
        logger.warning(
            "guard_sql_bloqueou",
            extra={"usuario_id": usuario_id, "motivo": str(exc), "sql": sql},
        )
        return {
            "resposta": "Não foi possível executar a consulta. Tente reformular a pergunta.",
            "dados": [],
            "sql": None,
            "total": 0,
            "erro": str(exc),
        }
    except psycopg2.Error as exc:
        logger.error(
            "erro_banco",
            extra={"usuario_id": usuario_id, "erro": str(exc), "sql": sql},
        )
        return {
            "resposta": "Ocorreu um erro ao consultar o banco de dados. Tente novamente.",
            "dados": [],
            "sql": None,
            "total": 0,
            "erro": f"Erro no banco: {exc}",
        }

    # ------------------------------------------------------------------
    # Etapa 3: Interpretação (com histórico para multi-turn)
    # ------------------------------------------------------------------
    try:
        resposta_final = _interpretar_resultado(pergunta, colunas, dados, sql, historico)
    except Exception as exc:
        logger.error("falha_interpretar", extra={"usuario_id": usuario_id, "erro": str(exc)})
        resposta_final = f"A consulta retornou {len(dados)} registro(s). ({explicacao})"

    # ------------------------------------------------------------------
    # Guard Rail de Saída 0 — modelo contradiz os dados
    # ------------------------------------------------------------------
    if _resposta_contradiz_dados(resposta_final, dados):
        logger.warning(
            "guard_contradicao_dados",
            extra={"usuario_id": usuario_id, "resposta_modelo": resposta_final},
        )
        resposta_final = _gerar_resposta_direta(dados)

    # ------------------------------------------------------------------
    # Guard Rail de Saída 1 — vazamento de informação interna
    # ------------------------------------------------------------------
    guard_vazamento = verificar_resposta_final(resposta_final)
    if not guard_vazamento.passou:
        logger.warning(
            "guard_vazamento_bloqueou",
            extra={"usuario_id": usuario_id, "motivo": guard_vazamento.motivo},
        )
        resposta_final = f"A consulta retornou {len(dados)} registro(s)."

    # ------------------------------------------------------------------
    # Guard Rail de Saída 2 — anti-alucinação numérica
    # ------------------------------------------------------------------
    guard_alucinacao = verificar_alucinacao(resposta_final, dados, historico)
    if not guard_alucinacao.passou:
        logger.warning(
            "guard_alucinacao_bloqueou",
            extra={"usuario_id": usuario_id, "motivo": guard_alucinacao.motivo},
        )
        resposta_final = (
            f"Foram encontrados {len(dados)} registro(s) para sua consulta. "
            f"Os dados estão disponíveis na tabela abaixo para sua análise."
        )

    duracao_ms = round((time.monotonic() - inicio) * 1000)
    logger.info(
        "consulta_concluida",
        extra={
            "usuario_id": usuario_id,
            "total_registros": len(dados),
            "duracao_ms": duracao_ms,
        },
    )

    return {
        "resposta": resposta_final,
        "dados": dados,
        "sql": sql,
        "total": len(dados),
        "erro": None,
    }
