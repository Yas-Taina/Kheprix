import os
from dotenv import load_dotenv

load_dotenv()

GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")
GROQ_MODEL: str   = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")

DW_HOST: str     = os.getenv("POSTGRES_DW_HOST", "db_dw")
DW_PORT: str     = os.getenv("POSTGRES_DW_PORT", "5432")
DW_USER: str     = os.getenv("POSTGRES_DW_USER", "kheprix_chatbot_ro")
DW_PASSWORD: str = os.getenv("POSTGRES_DW_PASSWORD", "chatbot_senha_local")
DW_NAME: str     = os.getenv("POSTGRES_DW_DB", "kheprix_dw_db")

DW_URL: str = (
    f"host={DW_HOST} port={DW_PORT} dbname={DW_NAME} "
    f"user={DW_USER} password={DW_PASSWORD}"
)

DW_POOL_MIN: int = 1
DW_POOL_MAX: int = 5

# gere com: python -c "import secrets; print(secrets.token_hex(32))"
CHATBOT_INTERNAL_KEY: str = os.getenv("CHATBOT_INTERNAL_KEY", "")

_variaveis_obrigatorias = {
    "GROQ_API_KEY": GROQ_API_KEY,
    "CHATBOT_INTERNAL_KEY": CHATBOT_INTERNAL_KEY,
}

_ausentes = [nome for nome, valor in _variaveis_obrigatorias.items() if not valor]
if _ausentes:
    raise RuntimeError(
        f"Variáveis de ambiente obrigatórias não configuradas: {', '.join(_ausentes)}.\n"
        "Consulte o README do chatbot para instruções de configuração."
    )
