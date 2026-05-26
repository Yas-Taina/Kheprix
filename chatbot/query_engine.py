import datetime
import decimal
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

_client = OpenAI(
    api_key=GROQ_API_KEY,
    base_url="https://api.groq.com/openai/v1",
)

_ERROS_TRANSIENTES = {429, 500, 502, 503, 504}
_MAX_RETRIES = 2
_RETRY_DELAY_S = 4


def _chamar_llm_com_retry(**kwargs) -> object:
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


def _gerar_sql(pergunta: str, historico: list[dict]) -> dict:
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


def _serializar_valor(v: object) -> object:
    if isinstance(v, (datetime.date, datetime.datetime)):
        return v.isoformat()
    if isinstance(v, decimal.Decimal):
        return float(v)
    return v


def _executar_sql(sql: str, estudo_ids: list[int]) -> tuple[list[str], list[dict]]:
    validar_sql(sql)

    resultado_guard = verificar_sql_output(sql)
    if not resultado_guard.passou:
        raise ValueError(f"Guard rail SQL: {resultado_guard.motivo}")

    sql = injetar_limit_se_ausente(sql)

    with obter_conexao() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, {"estudo_ids": estudo_ids})
            colunas = [desc.name for desc in cur.description] if cur.description else []
            linhas = [
                {k: _serializar_valor(v) for k, v in row.items()}
                for row in cur.fetchall()
            ]

    return colunas, linhas


def _resposta_contradiz_dados(resposta: str, dados: list[dict]) -> bool:
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
    return re.sub(
        r'((?:=|ILIKE|LIKE|IN|<>|!=)\s*)"([^"]*)"',
        r"\1'\2'",
        sql,
        flags=re.IGNORECASE,
    )


def _escapar_percent_sql(sql: str) -> str:
    """Dobra % em ILIKE/LIKE — psycopg2 interpreta % como início de placeholder."""
    def _dobrar(match: re.Match) -> str:
        return f"{match.group(1)} '{match.group(2).replace('%', '%%')}'"

    return re.sub(r"(ILIKE|LIKE)\s+'([^']*)'", _dobrar, sql, flags=re.IGNORECASE)


def _enriquecer_sql_especies(sql: str) -> str:
    # fallback para quando o modelo gera COUNT sem STRING_AGG ou SELECT sem DISTINCT
    tem_filtro_ameaca = bool(re.search(r"is_ameacada", sql, re.IGNORECASE))

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


def _interpretar_resultado(
    pergunta: str,
    colunas: list[str],
    dados: list[dict],
    sql: str,
    historico: list[dict],
) -> str:
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


def processar_pergunta(
    pergunta: str,
    estudo_ids: list[int],
    usuario_id: int,
    historico: list[dict] | None = None,
) -> dict:
    inicio = time.monotonic()
    historico = historico or []

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

    try:
        resultado_modelo = _gerar_sql(pergunta, historico)
    except APIStatusError as exc:
        if exc.status_code == 429:
            logger.warning("rate_limit_groq", extra={"usuario_id": usuario_id})
            return {
                "resposta": "O limite de uso do serviço de IA foi atingido por hoje. Tente novamente mais tarde.",
                "dados": [],
                "sql": None,
                "total": 0,
                "erro": f"Rate limit: {exc.status_code}",
            }
        logger.error("falha_gerar_sql", extra={"usuario_id": usuario_id, "erro": str(exc)})
        return {
            "resposta": "O serviço de IA está temporariamente indisponível. Tente novamente em instantes.",
            "dados": [],
            "sql": None,
            "total": 0,
            "erro": f"Falha ao gerar SQL: {exc}",
        }
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
        resposta_sem_sql = explicacao or (
            "Não foi possível responder a essa pergunta com os dados disponíveis. "
            "As informações de data disponíveis são: data de início de campanha (data_inicio_campanha) "
            "e data de cada registro de coleta (data_registro). "
            "Tente perguntar, por exemplo: 'Qual o período do meu estudo mais antigo?' ou "
            "'Quando foram feitos os primeiros registros?'"
        )
        return {"resposta": resposta_sem_sql, "dados": [], "sql": None, "total": 0, "erro": None}

    sql = _corrigir_aspas_sql(sql)
    sql = _escapar_percent_sql(sql)
    sql = _enriquecer_sql_especies(sql)

    try:
        colunas, dados = _executar_sql(sql, estudo_ids)
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

    try:
        resposta_final = _interpretar_resultado(pergunta, colunas, dados, sql, historico)
    except Exception as exc:
        logger.error("falha_interpretar", extra={"usuario_id": usuario_id, "erro": str(exc)})
        resposta_final = f"A consulta retornou {len(dados)} registro(s). ({explicacao})"

    if _resposta_contradiz_dados(resposta_final, dados):
        logger.warning(
            "guard_contradicao_dados",
            extra={"usuario_id": usuario_id, "resposta_modelo": resposta_final},
        )
        resposta_final = _gerar_resposta_direta(dados)

    guard_vazamento = verificar_resposta_final(resposta_final)
    if not guard_vazamento.passou:
        logger.warning(
            "guard_vazamento_bloqueou",
            extra={"usuario_id": usuario_id, "motivo": guard_vazamento.motivo},
        )
        resposta_final = f"A consulta retornou {len(dados)} registro(s)."

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
        f"consulta_concluida | usuario={usuario_id} | registros={len(dados)} | duracao_ms={duracao_ms} | sql={sql}",
    )

    return {
        "resposta": resposta_final,
        "dados": dados,
        "sql": sql,
        "total": len(dados),
        "erro": None,
    }
