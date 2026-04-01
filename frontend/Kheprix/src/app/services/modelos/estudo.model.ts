export interface VariavelInput {
  nome: string;
  nivel_aplicacao: string;
  tipo_dado: string;
  metrica?: string;
}

export interface EstudoListItem {
  id: number;
  nome: string;
  observacoes: string;
  perfil: string;
  created_at: string;
  updated_at: string;
}

export interface EstudoFiltros {
  nome?: string;
  criado_a_partir_de?: string;
  criado_ate?: string;
  atualizado_a_partir_de?: string;
  atualizado_ate?: string;
}

export interface CriarEstudoPayload {
  nome: string;
  observacoes?: string;
  variaveis: VariavelInput[];
}

export interface EstudoCriadoResponse {
  id: number;
  nome: string;
  observacoes: string;
  created_at: string;
  updated_at: string;
}

export interface DeletarEstudoProprietarioResponse {
  // 204 No Content — sem corpo
}

export interface DeletarEstudoColaboradorResponse {
  mensagem: string;
}
