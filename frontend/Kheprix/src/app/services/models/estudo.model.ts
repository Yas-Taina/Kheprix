export interface VariavelEstudo {
  nome: string;
  nivel_aplicacao: string;
  tipo_dado: string;
  metrica?: string;
}

export interface EstudoResumo {
  id: number;
  nome: string;
  observacoes: string;
  perfil: string;
  created_at: string;
  updated_at: string;
}

export interface EstudoDetalhe extends EstudoResumo {}

export interface ListarEstudosParams {
  nome?: string;
  criado_a_partir_de?: string;
  criado_ate?: string;
}

export interface CriarEstudoRequest {
  nome: string;
  observacoes?: string;
  variaveis: VariavelEstudo[];
}

export interface CriarEstudoResponse {
  id: number;
  nome: string;
  observacoes: string;
  created_at: string;
}
