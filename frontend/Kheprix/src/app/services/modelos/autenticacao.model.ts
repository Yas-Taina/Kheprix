export interface LoginPayload {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
}

export interface SolicitarRedefinicaoPayload {
  email: string;
}

export interface MensagemResponse {
  mensagem: string;
}

export interface ValidarTokenRedefinicaoPayload {
  token: string;
}

export interface ValidarTokenRedefinicaoResponse {
  valido: boolean;
}

export interface RedefinirSenhaPayload {
  token: string;
  nova_senha: string;
}

export interface AutocadastroPayload {
  nome: string;
  email: string;
  senha: string;
}

export interface UsuarioCadastradoResponse {
  id: number;
  nome: string;
  email: string;
  created_at: string;
}
