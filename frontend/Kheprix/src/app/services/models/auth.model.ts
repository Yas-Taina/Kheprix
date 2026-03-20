export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
}

export interface SolicitarRedefinicaoRequest {
  email: string;
}

export interface SolicitarRedefinicaoResponse {
  mensagem: string;
}

export interface ValidarTokenRedefinicaoRequest {
  token: string;
}

export interface ValidarTokenRedefinicaoResponse {
  valido: boolean;
}

export interface RedefinirSenhaRequest {
  token: string;
  nova_senha: string;
}

export interface RedefinirSenhaResponse {
  mensagem: string;
}

export interface AutocadastroRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface AutocadastroResponse {
  id: number;
  nome: string;
  email: string;
  created_at: string;
}
