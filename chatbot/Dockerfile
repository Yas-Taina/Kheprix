FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8001

# --workers 1 é obrigatório: session_store e rate_limiter usam estado em memória.
# Múltiplos workers não compartilham esse estado — cada worker teria suas próprias
# sessões e contadores de rate limit independentes, quebrando o comportamento esperado.
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "1", "--log-level", "info"]
