export interface EventoAmostragem {
  id: number;
  unidade_amostral_id: number;
  horario_inicio: string;
  horario_fim: string;
  esforco_real: string;
  created_at: string;
}

export interface CriarEventoAmostragemPayload {
  horario_inicio: string;
  horario_fim?: string;
  esforco_real?: string;
}

export interface AtualizarEventoAmostragemPayload {
  horario_inicio: string;
  horario_fim?: string;
  esforco_real?: string;
}
