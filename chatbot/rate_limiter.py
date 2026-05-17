# rate limiter em memória — suficiente para processo único; em produção substituir por Redis
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from threading import Lock

_LIMITE_POR_JANELA = 10
_JANELA = timedelta(minutes=1)

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
    agora = datetime.now(tz=timezone.utc)

    with _lock:
        historico = _historico[usuario_id]
        historico[:] = [t for t in historico if agora - t < _JANELA]

        if len(historico) >= _LIMITE_POR_JANELA:
            raise RateLimitExcedido(
                usuario_id=usuario_id,
                tentativas=len(historico),
                limite=_LIMITE_POR_JANELA,
            )

        historico.append(agora)


def requisicoes_restantes(usuario_id: int) -> int:
    agora = datetime.now(tz=timezone.utc)
    with _lock:
        historico = _historico[usuario_id]
        historico[:] = [t for t in historico if agora - t < _JANELA]
        return max(0, _LIMITE_POR_JANELA - len(historico))
