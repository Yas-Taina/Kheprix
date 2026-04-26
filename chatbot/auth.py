"""
Autenticação Serviço-a-Serviço
================================
O chatbot é um microserviço interno — apenas o backend Rails pode chamá-lo.
Isso é garantido por uma chave compartilhada (CHATBOT_INTERNAL_KEY) que o
Rails envia no header X-Internal-Key em toda requisição.

Modelo de segurança multi-tenant:
  - O Rails valida o JWT do usuário logado.
  - O Rails resolve os IDs dos estudos que o usuário pode acessar
    consultando a tabela collaboradores do OLTP.
  - O Rails envia {usuario_id, estudo_ids, pergunta} + X-Internal-Key ao chatbot.
  - O chatbot valida a chave e usa estudo_ids como filtro paramétrico
    no SQL — nunca interpola os IDs diretamente na string SQL.
  - Nenhum cliente externo consegue chamar o chatbot sem a chave interna.
"""
import secrets
from fastapi import Header, HTTPException, status
from config import CHATBOT_INTERNAL_KEY


async def verificar_chave_interna(
    x_internal_key: str = Header(..., description="Chave de autenticação serviço-a-serviço"),
) -> None:
    """
    Dependency do FastAPI — valida o header X-Internal-Key.
    Usa comparação em tempo constante (secrets.compare_digest) para
    evitar timing attacks.
    """
    if not secrets.compare_digest(x_internal_key, CHATBOT_INTERNAL_KEY):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Acesso não autorizado. Chave de serviço inválida.",
        )
