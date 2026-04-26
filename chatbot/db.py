"""
Connection Pool — DW
====================
Usa ThreadedConnectionPool do psycopg2 para reutilizar conexões
entre requests, evitando o overhead de abrir/fechar conexão a cada chamada.

Todas as conexões são configuradas como READ ONLY ao serem entregues pelo
pool, garantindo que o chatbot nunca consiga modificar dados no DW mesmo
que um SQL inválido escape dos guard rails.
"""
import psycopg2
import psycopg2.pool
import psycopg2.extras
from contextlib import contextmanager

from config import DW_URL, DW_POOL_MIN, DW_POOL_MAX

_pool: psycopg2.pool.ThreadedConnectionPool | None = None


def inicializar_pool() -> None:
    """Deve ser chamado uma vez na inicialização da aplicação (lifespan)."""
    global _pool
    _pool = psycopg2.pool.ThreadedConnectionPool(
        minconn=DW_POOL_MIN,
        maxconn=DW_POOL_MAX,
        dsn=DW_URL,
    )


def encerrar_pool() -> None:
    """Fecha todas as conexões do pool. Chamado no shutdown da aplicação."""
    if _pool:
        _pool.closeall()


@contextmanager
def obter_conexao():
    """
    Context manager que entrega uma conexão do pool e a devolve ao final.
    A conexão é configurada como read-only + autocommit ao ser retirada do
    pool — usando a API pública do psycopg2, sem acessar atributos privados.
    Garante que a conexão é sempre devolvida mesmo em caso de exceção.
    """
    if _pool is None:
        raise RuntimeError("Pool de conexões não inicializado.")

    conn = _pool.getconn()
    try:
        # set_session só pode ser chamado fora de uma transação.
        # conn.readonly é False por padrão; verificamos para evitar
        # chamar set_session desnecessariamente em conexões já configuradas.
        if not conn.readonly:
            conn.set_session(readonly=True, autocommit=True)
        yield conn
    finally:
        _pool.putconn(conn)
