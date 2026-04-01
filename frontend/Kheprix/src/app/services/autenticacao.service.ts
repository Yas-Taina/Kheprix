import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import {
  LoginPayload,
  LoginResponse,
  SolicitarRedefinicaoPayload,
  MensagemResponse,
  ValidarTokenRedefinicaoPayload,
  ValidarTokenRedefinicaoResponse,
  RedefinirSenhaPayload,
  AutocadastroPayload,
  UsuarioCadastradoResponse,
} from './modelos/autenticacao.model';

@Injectable({
  providedIn: 'root',
})
export class AutenticacaoService extends BaseService {
  private readonly TOKEN_KEY = 'auth_token';

  constructor(http: HttpClient) {
    super(http);
  }

  // POST /autenticacao/login
  login(payload: LoginPayload): Observable<LoginResponse> {
    return this.post<LoginResponse>('/autenticacao/login', payload).pipe(
      tap((res) => localStorage.setItem(this.TOKEN_KEY, res.token))
    );
  }

  // POST /autenticacao/solicitar_redefinicao
  solicitarRedefinicao(payload: SolicitarRedefinicaoPayload): Observable<MensagemResponse> {
    return this.post<MensagemResponse>('/autenticacao/solicitar_redefinicao', payload);
  }

  // POST /autenticacao/validar_token_redefinicao
  validarTokenRedefinicao(
    payload: ValidarTokenRedefinicaoPayload
  ): Observable<ValidarTokenRedefinicaoResponse> {
    return this.post<ValidarTokenRedefinicaoResponse>(
      '/autenticacao/validar_token_redefinicao',
      payload
    );
  }

  // POST /autenticacao/redefinir_senha
  redefinirSenha(payload: RedefinirSenhaPayload): Observable<MensagemResponse> {
    return this.post<MensagemResponse>('/autenticacao/redefinir_senha', payload);
  }

  // Logout — remove token localmente (não há endpoint de logout na API)
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  // Verifica se o usuário está autenticado
  isLogado(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  // Retorna o token armazenado
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  // POST /usuarios/autocadastro
  autocadastro(payload: AutocadastroPayload): Observable<UsuarioCadastradoResponse> {
    return this.post<UsuarioCadastradoResponse>('/usuarios/autocadastro', payload);
  }
}
