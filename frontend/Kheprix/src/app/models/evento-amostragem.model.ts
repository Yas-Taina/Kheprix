import { ValorVariavel } from "./variavel.model";

export interface EventoAmostragem {
  id: number;
  unidade_amostral_id: number;
  horario_inicio: string;
  esforco_real: string;
  created_at: string;
  //updated_at: string;
}

export interface EventoAmostragemCreate {
  horario_inicio: string;
  esforco_real?: string;
  valores_variaveis?: ValorVariavel[];
}

export interface EventoAmostragemUpdate extends EventoAmostragemCreate {}
