import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, of } from "rxjs";
import { tap } from "rxjs/operators";
import { BaseService } from "./base.service";
import {
  LoginRequest,
  LoginResponse,
  AutocadastroRequest,
  Usuario,
} from "../../models";

@Injectable({ providedIn: "root" })
export class AuthService extends BaseService {
  private readonly TOKEN_KEY = "kheprix_token";

  constructor(http: HttpClient) {
    super(http);
  }

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.post<LoginResponse>("/autenticacao/login", data).pipe(
      tap((res) => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  autocadastro(data: AutocadastroRequest): Observable<Usuario> {
    return this.post<Usuario>("/usuarios/autocadastro", data);
  }

  solicitarRedefinicao(email: string): Observable<{ mensagem: string }> {
    return this.post<{ mensagem: string }>(
      "/autenticacao/solicitar_redefinicao",
      { email },
    );
  }

  validarTokenRedefinicao(token: string): Observable<{ valido: boolean }> {
    return this.post<{ valido: boolean }>(
      "/autenticacao/validar_token_redefinicao",
      { token },
    );
  }

  redefinirSenha(
    token: string,
    nova_senha: string,
  ): Observable<{ mensagem: string }> {
    return this.post<{ mensagem: string }>("/autenticacao/redefinir_senha", {
      token,
      nova_senha,
    });
  }

  validarToken(token: string): Observable<{ valido: boolean }> {
    return this.post<{ valido: boolean }>("/autenticacao/validar_token", {
      token,
    });
  }
}
