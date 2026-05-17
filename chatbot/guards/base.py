from dataclasses import dataclass


@dataclass
class GuardResult:
    passou: bool
    motivo: str | None = None
