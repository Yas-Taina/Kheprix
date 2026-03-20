import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { BaseService } from './base.service';
import {
  LoginRequest,
  LoginResponse,
  SolicitarRedefinicaoRequest,
  SolicitarRedefinicaoResponse,
  ValidarTokenRedefinicaoRequest,
  ValidarTokenRedefinicaoResponse,
  RedefinirSenhaRequest,
  RedefinirSenhaResponse,
  AutocadastroRequest,
  AutocadastroResponse,
} from './models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService extends BaseService {

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.post<LoginResponse>('/autenticacao/login', body).pipe(
      tap((res) => this.saveToken(res.token))
    );
  }

  logout(): void {
    this.removeToken();
  }

  solicitarRedefinicao(body: SolicitarRedefinicaoRequest): Observable<SolicitarRedefinicaoResponse> {
    return this.post<SolicitarRedefinicaoResponse>('/autenticacao/solicitar_redefinicao', body);
  }

  validarTokenRedefinicao(body: ValidarTokenRedefinicaoRequest): Observable<ValidarTokenRedefinicaoResponse> {
    return this.post<ValidarTokenRedefinicaoResponse>('/autenticacao/validar_token_redefinicao', body);
  }

  redefinirSenha(body: RedefinirSenhaRequest): Observable<RedefinirSenhaResponse> {
    return this.post<RedefinirSenhaResponse>('/autenticacao/redefinir_senha', body);
  }

  autocadastro(body: AutocadastroRequest): Observable<AutocadastroResponse> {
    return this.post<AutocadastroResponse>('/usuarios/autocadastro', body);
  }
}
