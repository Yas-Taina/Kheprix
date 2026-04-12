export interface Colaborador {
  id_usuario: number;
  nome: string;
  email: string;
  perfil: string;
}

export interface AtualizarColaboradorPayload {
  perfil: string;
}

// Convites enviados (proprietário)
export interface EnviarConvitePayload {
  email_convidado: string;
}

export interface ConviteEnviado {
  id: number;
  estudo_id: number;
  email_convidado: string;
  token: string;
  status: string;
  data_expiracao: string;
  created_at: string;
}

export interface ConviteListItem {
  id: number;
  email_convidado: string;
  status: string;
  data_expiracao: string;
  created_at: string;
}

// Convites recebidos (autenticado / público)
export interface ConviteRecebidoListItem {
  id: number;
  estudo_id: number;
  nome_estudo: string;
  nome_remetente: string;
  status: string;
  data_expiracao: string;
  created_at: string;
}

export interface ConviteRecebidoDetalhe {
  id: number;
  estudo_id: number;
  nome_estudo: string;
  email_convidado: string;
  status: string;
  data_expiracao: string;
}

// Código de acesso
export interface CodigoAcesso {
  codigo: string;
  senha_autocadastro: string;
}

export interface AtualizarCodigoAcessoPayload {
  senha_autocadastro: string;
}

// Autocadastro em estudo
export interface IngressarEstudoPayload {
  codigo: string;
  senha_autocadastro: string;
}

export interface IngressarEstudoResponse {
  estudo_id: number;
  nome_estudo: string;
  perfil: string;
}
