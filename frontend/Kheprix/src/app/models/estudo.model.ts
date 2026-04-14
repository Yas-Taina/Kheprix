import { VariavelCreate } from './variavel.model';

export interface Estudo {
  id: number;
  nome: string;
  observacoes: string;
  perfil?: string;
  created_at: string;
  updated_at: string;
}

export interface EstudoCreate {
  nome: string;
  observacoes?: string;
  variaveis: VariavelCreate[];
}

export interface EstudoUpdate {
  nome: string;
  observacoes?: string;
}

export type TipoAgrupamento =
  | 'registro_ocorrencia'
  | 'evento_amostragem'
  | 'unidade_amostral'
  | 'campanha'
  | 'especie';
