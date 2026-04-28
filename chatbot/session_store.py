"""
Gerenciamento de sessão em memória para o chatbot Kheprix.

Mantém o histórico de perguntas e respostas por usuário para permitir
conversas multi-turn ("e dessas, quais são ameaçadas?").

Características:
  - Thread-safe (usa Lock)
  - TTL de 30 minutos por inatividade
  - Máximo de MAX_TURNS pares de pergunta/resposta por sessão
  - Sem persistência — reinicia junto com o container (intencional)
"""
import threading
import time
from dataclasses import dataclass, field

SESSION_TTL_S = 30 * 60   # 30 minutos de inatividade → sessão expirada
MAX_TURNS     = 4          # últimas 4 trocas (8 mensagens) mantidas


@dataclass
class _Sessao:
    # Lista de mensagens no formato OpenAI: [{"role": "user"|"assistant", "content": str}]
    mensagens: list[dict] = field(default_factory=list)
    ultimo_acesso: float = field(default_factory=time.monotonic)


class SessionStore:
    """Store de sessões em memória, thread-safe."""

    def __init__(self) -> None:
        self._store: dict[int, _Sessao] = {}
        self._lock = threading.Lock()

    def obter_historico(self, usuario_id: int) -> list[dict]:
        """Retorna o histórico de mensagens da sessão do usuário.

        Retorna lista vazia se a sessão não existe ou expirou.
        """
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
        """Adiciona um par pergunta/resposta à sessão do usuário.

        Mantém apenas os últimos MAX_TURNS pares (janela deslizante).
        """
        with self._lock:
            if usuario_id not in self._store:
                self._store[usuario_id] = _Sessao()
            sessao = self._store[usuario_id]
            sessao.mensagens.append({"role": "user",      "content": pergunta})
            sessao.mensagens.append({"role": "assistant", "content": resposta})
            # Janela deslizante: mantém apenas os últimos MAX_TURNS pares
            limite = MAX_TURNS * 2
            if len(sessao.mensagens) > limite:
                sessao.mensagens = sessao.mensagens[-limite:]
            sessao.ultimo_acesso = time.monotonic()

    def limpar(self, usuario_id: int) -> None:
        """Remove a sessão do usuário (ex: logout)."""
        with self._lock:
            self._store.pop(usuario_id, None)


# Instância global compartilhada pelo app
session_store = SessionStore()
