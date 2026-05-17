import psycopg2
import psycopg2.pool
from contextlib import contextmanager

from config import DW_URL, DW_POOL_MIN, DW_POOL_MAX

_pool: psycopg2.pool.ThreadedConnectionPool | None = None


def inicializar_pool() -> None:
    global _pool
    _pool = psycopg2.pool.ThreadedConnectionPool(
        minconn=DW_POOL_MIN,
        maxconn=DW_POOL_MAX,
        dsn=DW_URL,
    )


def encerrar_pool() -> None:
    if _pool:
        _pool.closeall()


@contextmanager
def obter_conexao():
    if _pool is None:
        raise RuntimeError("Pool de conexões não inicializado.")

    conn = _pool.getconn()
    try:
        # set_session só pode ser chamado fora de uma transação;
        # verificamos conn.readonly para não chamar desnecessariamente em conexões já configuradas
        if not conn.readonly:
            conn.set_session(readonly=True, autocommit=True)
        yield conn
    finally:
        _pool.putconn(conn)
