"""
guards — Guard Rails do Chatbot Kheprix
=========================================
Pacote com todas as camadas de validação e proteção do pipeline de IA.
Cada módulo cobre uma fase distinta do fluxo de dados:

  base.py           Tipos compartilhados (GuardResult).

  input_guard.py    Valida a ENTRADA do usuário antes de chamar o modelo.
                    → Detecta prompt injection e perguntas fora do domínio.

  sql_validator.py  Valida o SQL gerado pelo modelo (1ª camada).
                    → Garante apenas SELECT, filtro multi-tenant e tabelas autorizadas.

  output_guard.py   Valida as SAÍDAS do modelo (2ª camada de SQL + resposta final).
                    → Bloqueia tabelas de sistema, funções proibidas, stacked queries,
                      vazamento de informação interna e alucinação numérica.

Ordem de execução no pipeline (query_engine.py):
  1. input_guard.validar_entrada()
  2. [LLM Groq/Llama — geração de SQL]
  3. sql_validator.validar_sql()
  4. output_guard.verificar_sql_output()
  5. output_guard.injetar_limit_se_ausente()
  6. [execução no DW — read-only, filtro multi-tenant paramétrico]
  7. [LLM Groq/Llama — interpretação do resultado]
  8. query_engine._resposta_contradiz_dados()
  9. output_guard.verificar_resposta_final()
 10. output_guard.verificar_alucinacao()
"""

from guards.base import GuardResult
from guards.input_guard import validar_entrada
from guards.sql_validator import validar_sql
from guards.output_guard import (
    injetar_limit_se_ausente,
    verificar_sql_output,
    verificar_resposta_final,
    verificar_alucinacao,
)

__all__ = [
    "GuardResult",
    "validar_entrada",
    "validar_sql",
    "injetar_limit_se_ausente",
    "verificar_sql_output",
    "verificar_resposta_final",
    "verificar_alucinacao",
]
