export interface ValorVariavel {
  variavel_id: number;
  valor: string;
}

export interface Campanha {
  id: number;
  nome: string;
  data_inicio: string;
  data_fim: string;
  descricao: string;
  created_at: string;
  updated_at: string;
}

export interface CriarCampanhaPayload {
  nome: string;
  data_inicio: string;
  data_fim?: string;
  descricao?: string;
  valores_variaveis?: ValorVariavel[];
}

export interface AtualizarCampanhaPayload {
  nome: string;
  data_inicio: string;
  data_fim: string;
  descricao: string;
  valores_variaveis?: ValorVariavel[];
}
