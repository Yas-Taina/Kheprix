"""
Kheprix Chatbot — FastAPI App
==============================
Expõe o endpoint POST /query protegido por:
  - Autenticação serviço-a-serviço (X-Internal-Key)
  - Rate limiting por usuário (10 req/min)
  - Guard rails de entrada e saída (via query_engine)
  - Multi-tenant enforcement (estudo_ids paramétricos no SQL)
"""
import logging
import logging.config
from contextlib import asynccontextmanager

from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from auth import verificar_chave_interna
from db import inicializar_pool, encerrar_pool
from rate_limiter import verificar_rate_limit, RateLimitExcedido, requisicoes_restantes
from query_engine import processar_pergunta
from insights_engine import gerar_insights
from session_store import session_store

# ---------------------------------------------------------------------------
# Logging estruturado
# ---------------------------------------------------------------------------
logging.config.dictConfig({
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json": {
            "format": '{"time":"%(asctime)s","level":"%(levelname)s","logger":"%(name)s","msg":"%(message)s"}',
            "datefmt": "%Y-%m-%dT%H:%M:%S",
        }
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
        }
    },
    "root": {"level": "INFO", "handlers": ["console"]},
})

logger = logging.getLogger("kheprix.chatbot")

# ---------------------------------------------------------------------------
# Lifespan — inicializa e encerra o connection pool junto com a aplicação
# ---------------------------------------------------------------------------
@asynccontextmanager
async def lifespan(_app: FastAPI):
    logger.info("chatbot_iniciando — abrindo connection pool com o DW")
    inicializar_pool()
    yield
    logger.info("chatbot_encerrando — fechando connection pool")
    encerrar_pool()


# ---------------------------------------------------------------------------
# App
# ---------------------------------------------------------------------------
app = FastAPI(
    title="Kheprix Chatbot IA",
    description="Consultas em linguagem natural ao Data Warehouse de entomologia. Uso interno.",
    version="1.0.0",
    lifespan=lifespan,
    docs_url=None,       # desabilita Swagger público
    redoc_url=None,
    openapi_url=None,
)


# ---------------------------------------------------------------------------
# Modelos de request / response
# ---------------------------------------------------------------------------
class QueryRequest(BaseModel):
    pergunta: str = Field(..., min_length=5, max_length=500)
    estudo_ids: list[int] = Field(..., min_length=1)
    usuario_id: int = Field(..., description="ID do usuário autenticado (validado pelo Rails)")


class QueryResponse(BaseModel):
    resposta: str | None
    dados: list[dict]
    sql: str | None
    total: int
    erro: str | None


class InsightsRequest(BaseModel):
    estudo_ids: list[int] = Field(..., min_length=1)
    usuario_id: int = Field(..., description="ID do usuário autenticado (validado pelo Rails)")


class InsightsResponse(BaseModel):
    narrativa: str
    metricas: dict
    erro: str | None


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------
@app.get("/health")
def health():
    return {"status": "ok", "servico": "kheprix-chatbot"}


@app.post(
    "/query",
    response_model=QueryResponse,
    dependencies=[Depends(verificar_chave_interna)],
)
def query(body: QueryRequest):
    # Rate limiting por usuário
    try:
        verificar_rate_limit(body.usuario_id)
    except RateLimitExcedido as exc:
        logger.warning(
            "rate_limit_excedido",
            extra={"usuario_id": exc.usuario_id, "tentativas": exc.tentativas},
        )
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=f"Limite de {exc.limite} perguntas por minuto atingido. Aguarde e tente novamente.",
            headers={"Retry-After": "60"},
        )

    logger.info(
        "requisicao_recebida",
        extra={
            "usuario_id": body.usuario_id,
            "pergunta_len": len(body.pergunta),
            "qtd_estudos": len(body.estudo_ids),
        },
    )

    # Recupera histórico da sessão para multi-turn context
    historico = session_store.obter_historico(body.usuario_id)

    resultado = processar_pergunta(
        pergunta=body.pergunta,
        estudo_ids=body.estudo_ids,
        usuario_id=body.usuario_id,
        historico=historico,
    )

    # Salva o turno na sessão apenas se a resposta foi bem-sucedida
    if resultado.get("erro") is None and resultado.get("resposta"):
        session_store.adicionar_turno(
            usuario_id=body.usuario_id,
            pergunta=body.pergunta,
            resposta=resultado["resposta"],
        )

    restantes = requisicoes_restantes(body.usuario_id)

    response = JSONResponse(
        content=resultado,
        headers={"X-RateLimit-Remaining": str(restantes)},
    )
    return response


@app.post(
    "/insights",
    response_model=InsightsResponse,
    dependencies=[Depends(verificar_chave_interna)],
)
def insights(body: InsightsRequest):
    """
    Gera um relatório analítico sobre os estudos do usuário.

    Diferente de /query, não aceita perguntas livres — executa um conjunto
    fixo de queries predefinidas e usa o LLM apenas para narrar os resultados.
    Isso garante SQL auditável e respostas consistentes.
    """
    try:
        verificar_rate_limit(body.usuario_id)
    except RateLimitExcedido as exc:
        logger.warning(
            "rate_limit_excedido_insights",
            extra={"usuario_id": exc.usuario_id, "tentativas": exc.tentativas},
        )
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=f"Limite de {exc.limite} perguntas por minuto atingido. Aguarde e tente novamente.",
            headers={"Retry-After": "60"},
        )

    logger.info(
        "insights_solicitados",
        extra={"usuario_id": body.usuario_id, "qtd_estudos": len(body.estudo_ids)},
    )

    resultado = gerar_insights(
        estudo_ids=body.estudo_ids,
        usuario_id=body.usuario_id,
    )

    restantes = requisicoes_restantes(body.usuario_id)
    return JSONResponse(
        content=resultado,
        headers={"X-RateLimit-Remaining": str(restantes)},
    )
