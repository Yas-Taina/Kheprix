# gera relatório analítico com SQL fixo (não aceita entrada livre) — auditável e sem risco de injeção
import json
import logging
import time

import psycopg2
import psycopg2.extras
from openai import OpenAI, APIStatusError

from config import GROQ_API_KEY, GROQ_MODEL
from db import obter_conexao

logger = logging.getLogger("kheprix.chatbot")

_client = OpenAI(
    api_key=GROQ_API_KEY,
    base_url="https://api.groq.com/openai/v1",
)

_ERROS_TRANSIENTES = {429, 500, 502, 503, 504}
_MAX_RETRIES = 2
_RETRY_DELAY_S = 4


_SQL_RESUMO_GERAL = """
SELECT
    COUNT(DISTINCT nome_cientifico)                        AS riqueza_total,
    SUM(quantidade)                                        AS abundancia_total,
    COUNT(*)                                               AS total_registros,
    COUNT(DISTINCT nome_campanha)                          AS total_campanhas,
    MIN(data_registro)::text                               AS primeira_coleta,
    MAX(data_registro)::text                               AS ultima_coleta,
    STRING_AGG(DISTINCT nome_estudo, ', ' ORDER BY nome_estudo) AS estudos
FROM public.indicadores_dashboard
WHERE fk_estudo = ANY(%(estudo_ids)s)
"""

_SQL_TOP_ESPECIES = """
SELECT
    nome_cientifico,
    nome_popular,
    SUM(quantidade)          AS abundancia,
    COUNT(DISTINCT nome_campanha) AS campanhas
FROM public.indicadores_dashboard
WHERE fk_estudo = ANY(%(estudo_ids)s)
GROUP BY nome_cientifico, nome_popular
ORDER BY abundancia DESC
LIMIT 5
"""

_SQL_CONSERVACAO = """
SELECT
    COUNT(DISTINCT nome_cientifico) FILTER (WHERE is_ameacada = true)  AS especies_ameacadas,
    COUNT(DISTINCT nome_cientifico) FILTER (WHERE is_endemica = true)  AS especies_endemicas,
    COUNT(DISTINCT nome_cientifico)                                     AS total_especies,
    STRING_AGG(DISTINCT nome_cientifico, ', ')
        FILTER (WHERE is_ameacada = true)                              AS nomes_ameacadas,
    STRING_AGG(DISTINCT status_conservacao, ', ')
        FILTER (WHERE is_ameacada = true AND status_conservacao != 'NA') AS status_iucn
FROM public.indicadores_dashboard
WHERE fk_estudo = ANY(%(estudo_ids)s)
"""

_SQL_SAZONALIDADE = """
SELECT
    estacao,
    SUM(quantidade)              AS total_individuos,
    COUNT(DISTINCT nome_cientifico) AS riqueza
FROM public.indicadores_dashboard
WHERE fk_estudo = ANY(%(estudo_ids)s)
  AND estacao IS NOT NULL
  AND estacao != 'NA'
GROUP BY estacao
ORDER BY total_individuos DESC
"""

_SQL_TAXONOMIA = """
SELECT
    ordem,
    COUNT(DISTINCT nome_cientifico) AS riqueza,
    SUM(quantidade)                 AS abundancia
FROM public.indicadores_dashboard
WHERE fk_estudo = ANY(%(estudo_ids)s)
  AND ordem IS NOT NULL
  AND ordem != 'NA'
GROUP BY ordem
ORDER BY riqueza DESC
LIMIT 8
"""

_PROMPT_INSIGHTS = """
Você é um ecólogo analítico do sistema Kheprix, especializado em entomologia.
Com base nas métricas coletadas do banco de dados, gere um relatório de insights
em português do Brasil para o pesquisador.

Estruture o relatório em exatamente 5 parágrafos curtos, nesta ordem:
1. Visão Geral: descreva o escopo — período, campanhas, riqueza e abundância total.
2. Espécies em Destaque: cite as espécies mais abundantes e seus valores numéricos exatos.
3. Conservação e Endemismo: cite espécies ameaçadas com status IUCN e proporção de endêmicas. Se não houver, diga isso.
4. Padrões Temporais: descreva a distribuição por estação com os valores exatos dos dados.
5. Composição Taxonômica: descreva as ordens presentes. Se não houver dados, diga isso.

## REGRAS ABSOLUTAS
- Cite APENAS valores que aparecem explicitamente nos dados fornecidos.
- NUNCA use markdown (sem #, ##, ###, **, *, _, ---, etc.). Texto puro apenas.
- NUNCA faça inferências, sugestões, recomendações ou afirmações como "isso pode indicar", "isso sugere", "pode ser explicado por".
- NUNCA mencione IDs de backend, nomes de tabelas ou SQL.
- Responda em português do Brasil.
"""



def _executar_query(sql: str, estudo_ids: list[int]) -> list[dict]:
    with obter_conexao() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, {"estudo_ids": estudo_ids})
            return [dict(row) for row in cur.fetchall()]


def _coletar_metricas(estudo_ids: list[int]) -> dict:
    metricas = {}
    queries = {
        "resumo":      _SQL_RESUMO_GERAL,
        "top_especies": _SQL_TOP_ESPECIES,
        "conservacao": _SQL_CONSERVACAO,
        "sazonalidade": _SQL_SAZONALIDADE,
        "taxonomia":   _SQL_TAXONOMIA,
    }

    for nome, sql in queries.items():
        try:
            resultado = _executar_query(sql, estudo_ids)
            metricas[nome] = resultado
        except psycopg2.Error as exc:
            logger.error(
                "insights_query_falhou",
                extra={"query": nome, "erro": str(exc)},
            )
            metricas[nome] = []

    return metricas



def _gerar_narrativa(metricas: dict, estudo_ids: list[int]) -> str:
    contexto = (
        f"Estudos consultados (IDs internos): {len(estudo_ids)} estudo(s)\n\n"
        f"=== Resumo Geral ===\n"
        f"{json.dumps(metricas.get('resumo', []), default=str, ensure_ascii=False)}\n\n"
        f"=== Top 5 Espécies por Abundância ===\n"
        f"{json.dumps(metricas.get('top_especies', []), default=str, ensure_ascii=False)}\n\n"
        f"=== Conservação e Endemismo ===\n"
        f"{json.dumps(metricas.get('conservacao', []), default=str, ensure_ascii=False)}\n\n"
        f"=== Registros por Estação ===\n"
        f"{json.dumps(metricas.get('sazonalidade', []), default=str, ensure_ascii=False)}\n\n"
        f"=== Composição Taxonômica (top ordens) ===\n"
        f"{json.dumps(metricas.get('taxonomia', []), default=str, ensure_ascii=False)}"
    )

    for tentativa in range(_MAX_RETRIES + 1):
        try:
            resposta = _client.chat.completions.create(
                model=GROQ_MODEL,
                messages=[
                    {"role": "system", "content": _PROMPT_INSIGHTS},
                    {"role": "user",   "content": contexto},
                ],
                temperature=0.2,
                max_tokens=900,
            )
            return resposta.choices[0].message.content
        except APIStatusError as exc:
            if exc.status_code in _ERROS_TRANSIENTES and tentativa < _MAX_RETRIES:
                logger.warning(
                    "insights_llm_retry",
                    extra={"tentativa": tentativa + 1, "status": exc.status_code},
                )
                time.sleep(_RETRY_DELAY_S * (tentativa + 1))
                continue
            raise



def gerar_insights(estudo_ids: list[int], usuario_id: int) -> dict:
    inicio = time.monotonic()

    try:
        metricas = _coletar_metricas(estudo_ids)
    except Exception as exc:
        logger.error("insights_coleta_falhou", extra={"usuario_id": usuario_id, "erro": str(exc)})
        return {
            "narrativa": "Não foi possível coletar as métricas dos estudos. Tente novamente.",
            "metricas": {},
            "erro": f"Falha na coleta de métricas: {exc}",
        }

    resumo = metricas.get("resumo", [{}])
    if not resumo or resumo[0].get("total_registros") in (None, 0):
        return {
            "narrativa": "Não foram encontrados registros nos estudos informados.",
            "metricas": metricas,
            "erro": None,
        }

    try:
        narrativa = _gerar_narrativa(metricas, estudo_ids)
    except APIStatusError as exc:
        if exc.status_code == 429:
            logger.warning("rate_limit_groq_insights", extra={"usuario_id": usuario_id})
            return {
                "narrativa": "O limite de uso do serviço de IA foi atingido por hoje. Tente novamente mais tarde.",
                "metricas": metricas,
                "erro": f"Rate limit: {exc.status_code}",
            }
        logger.error("insights_llm_falhou", extra={"usuario_id": usuario_id, "erro": str(exc)})
        narrativa = "Os dados foram coletados com sucesso, mas não foi possível gerar a narrativa analítica. Tente novamente em instantes."
    except Exception as exc:
        logger.error("insights_llm_falhou", extra={"usuario_id": usuario_id, "erro": str(exc)})
        narrativa = "Os dados foram coletados com sucesso, mas não foi possível gerar a narrativa analítica. Tente novamente em instantes."

    duracao_ms = round((time.monotonic() - inicio) * 1000)
    logger.info(
        "insights_gerados",
        extra={"usuario_id": usuario_id, "duracao_ms": duracao_ms},
    )

    return {
        "narrativa": narrativa,
        "metricas": metricas,
        "erro": None,
    }
