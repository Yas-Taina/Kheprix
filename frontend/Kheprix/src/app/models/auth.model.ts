export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
}

export interface RecuperacaoSenhaStep1Request {
  email: string;
}

export interface RecuperacaoSenhaStep2Request {
  token: string;
}

export interface RecuperacaoSenhaStep3Request {
  token: string;
  nova_senha: string;
}

export interface AutocadastroRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  created_at: string;
}
