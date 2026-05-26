import secrets
from fastapi import Header, HTTPException, status
from config import CHATBOT_INTERNAL_KEY


async def verificar_chave_interna(
    x_internal_key: str = Header(..., description="Chave de autenticação serviço-a-serviço"),
) -> None:
    if not secrets.compare_digest(x_internal_key, CHATBOT_INTERNAL_KEY):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Acesso não autorizado. Chave de serviço inválida.",
        )
