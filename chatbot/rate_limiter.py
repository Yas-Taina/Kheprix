"""
Rate Limiter por Usuário
========================
Limita o número de perguntas que um usuário pode fazer ao chatbot
dentro de uma janela de tempo deslizante.

Implementação em memória — suficiente para um único processo (TCC/dev).
Em produção, substituir por Redis para persistir entre instâncias.
"""
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from threading import Lock

# Configuração
_LIMITE_POR_JANELA = 10          # máximo de requisições por usuário
_JANELA = timedelta(minutes=1)   # janela deslizante de 1 minuto

_historico: dict[int, list[datetime]] = defaultdict(list)
_lock = Lock()


class RateLimitExcedido(Exception):
    def __init__(self, usuario_id: int, tentativas: int, limite: int):
        self.usuario_id = usuario_id
        self.tentativas = tentativas
        self.limite = limite
        super().__init__(
            f"Usuário {usuario_id} excedeu o limite de {limite} req/min "
            f"({tentativas} tentativas na janela atual)."
        )


def verificar_rate_limit(usuario_id: int) -> None:
    """
    Verifica e registra uma requisição do usuário.
    Levanta RateLimitExcedido se o limite foi atingido.
    """
    agora = datetime.now(tz=timezone.utc)

    with _lock:
        historico = _historico[usuario_id]

        # Remove timestamps fora da janela deslizante
        historico[:] = [t for t in historico if agora - t < _JANELA]

        if len(historico) >= _LIMITE_POR_JANELA:
            raise RateLimitExcedido(
                usuario_id=usuario_id,
                tentativas=len(historico),
                limite=_LIMITE_POR_JANELA,
            )

        historico.append(agora)


def requisicoes_restantes(usuario_id: int) -> int:
    """Retorna quantas requisições o usuário ainda pode fazer na janela atual."""
    agora = datetime.now(tz=timezone.utc)
    with _lock:
        historico = _historico[usuario_id]
        historico[:] = [t for t in historico if agora - t < _JANELA]
        return max(0, _LIMITE_POR_JANELA - len(historico))
