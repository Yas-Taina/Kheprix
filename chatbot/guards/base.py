"""
Tipos base compartilhados pelos guard rails.
"""
from dataclasses import dataclass


@dataclass
class GuardResult:
    """Resultado de uma verificação de guard rail."""
    passou: bool
    motivo: str | None = None
