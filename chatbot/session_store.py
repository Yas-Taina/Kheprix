# sessões em memória, thread-safe, TTL 30 min — sem persistência entre reinicializações (intencional)
import threading
import time
from dataclasses import dataclass, field

SESSION_TTL_S = 30 * 60
MAX_TURNS     = 4


@dataclass
class _Sessao:
    # formato OpenAI: [{"role": "user"|"assistant", "content": str}]
    mensagens: list[dict] = field(default_factory=list)
    ultimo_acesso: float = field(default_factory=time.monotonic)


class SessionStore:
    def __init__(self) -> None:
        self._store: dict[int, _Sessao] = {}
        self._lock = threading.Lock()

    def obter_historico(self, usuario_id: int) -> list[dict]:
        with self._lock:
            sessao = self._store.get(usuario_id)
            if sessao is None:
                return []
            if time.monotonic() - sessao.ultimo_acesso > SESSION_TTL_S:
                del self._store[usuario_id]
                return []
            sessao.ultimo_acesso = time.monotonic()
            return list(sessao.mensagens)

    def adicionar_turno(self, usuario_id: int, pergunta: str, resposta: str) -> None:
        with self._lock:
            if usuario_id not in self._store:
                self._store[usuario_id] = _Sessao()
            sessao = self._store[usuario_id]
            sessao.mensagens.append({"role": "user",      "content": pergunta})
            sessao.mensagens.append({"role": "assistant", "content": resposta})
            limite = MAX_TURNS * 2
            if len(sessao.mensagens) > limite:
                sessao.mensagens = sessao.mensagens[-limite:]
            sessao.ultimo_acesso = time.monotonic()

    def limpar(self, usuario_id: int) -> None:
        with self._lock:
            self._store.pop(usuario_id, None)


session_store = SessionStore()
