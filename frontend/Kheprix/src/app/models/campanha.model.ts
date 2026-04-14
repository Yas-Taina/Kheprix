import { ValorVariavel } from './variavel.model';

export interface Campanha {
  id: number;
  nome: string;
  data_inicio: string;
  data_fim: string;
  descricao: string;
  created_at: string;
  updated_at: string;
}

export interface CampanhaCreate {
  nome: string;
  data_inicio: string;
  data_fim?: string;
  descricao?: string;
  valores_variaveis?: ValorVariavel[];
}

export interface CampanhaUpdate extends CampanhaCreate {}
