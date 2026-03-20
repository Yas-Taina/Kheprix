export interface Especie {
  id: number;
  estudo_id: number;
  foto: string;
  classe: string;
  genero: string;
  nome_popular: string;
  nome_cientifico: string;
  status_conservacao: string;
  nativa_da_regiao: boolean;
  created_at: string;
}

export interface ListarEspeciesParams {
  nome_popular?: string;
  nome_cientifico?: string;
}

export interface CriarEspecieRequest {
  nome_cientifico: string;
  nome_popular?: string;
  foto?: string;
  classe?: string;
  genero?: string;
  status_conservacao?: string;
  nativa_da_regiao?: boolean;
}

export interface AtualizarEspecieRequest {
  nome_cientifico: string;
  nome_popular: string;
  foto: string;
  classe: string;
  genero: string;
  status_conservacao: string;
  nativa_da_regiao: boolean;
}
