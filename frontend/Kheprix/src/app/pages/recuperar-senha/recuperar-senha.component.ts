import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../core/services/auth.service";

@Component({
  selector: "app-recuperar-senha",
  standalone: true,
  templateUrl: "./recuperar-senha.component.html",
  styleUrls: ["./recuperar-senha.component.css"],
  imports: [CommonModule, FormsModule],
})
export class RecuperarSenhaComponent {
  email = "";
  token = "";
  novaSenha = "";
  confirmarSenha = "";
  tokenEnviado = false;
  tokenValido = false;
  loadingStep1 = false;
  loadingStep2 = false;
  loadingStep3 = false;
  mostrarSenha = false;
  mostrarSenhaC = false;
  erroStep1 = "";
  msgStep1 = "";
  erroStep3 = "";
  msgStep3 = "";

  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  private static readonly EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  private static readonly SENHA_MIN = 8;

  solicitarToken() {
    this.erroStep1 = "";
    if (!this.email) {
      this.erroStep1 = "Informe o e-mail.";
      return;
    }
    if (!RecuperarSenhaComponent.EMAIL_REGEX.test(this.email)) {
      this.erroStep1 = "E-mail inválido. Verifique o formato.";
      return;
    }
    this.loadingStep1 = true;
    this.auth.solicitarRedefinicao(this.email).subscribe({
      next: (res) => {
        this.msgStep1 =
          res.mensagem || "Token de recuperação enviado para o e-mail.";
        this.tokenEnviado = true;
        this.loadingStep1 = false;
      },
      error: () => {
        this.erroStep1 = "E-mail não encontrado.";
        this.loadingStep1 = false;
      },
    });
  }

  validarToken() {
    if (!this.token) {
      this.erroStep1 = "Informe o token.";
      return;
    }
    this.loadingStep2 = true;
    this.erroStep1 = "";
    this.auth.validarTokenRedefinicao(this.token).subscribe({
      next: (res) => {
        if (res.valido) {
          this.tokenValido = true;
        } else {
          this.erroStep1 = "Token inválido ou expirado.";
        }
        this.loadingStep2 = false;
      },
      error: () => {
        this.erroStep1 = "Erro ao validar token.";
        this.loadingStep2 = false;
      },
    });
  }

  redefinirSenha() {
    this.erroStep3 = "";
    if (!this.novaSenha) {
      this.erroStep3 = "Informe a nova senha.";
      return;
    }
    if (this.novaSenha.length < RecuperarSenhaComponent.SENHA_MIN) {
      this.erroStep3 = `A senha deve ter pelo menos ${RecuperarSenhaComponent.SENHA_MIN} caracteres.`;
      return;
    }
    if (this.novaSenha !== this.confirmarSenha) {
      this.erroStep3 = "As senhas não coincidem.";
      return;
    }
    this.loadingStep3 = true;
    this.auth.redefinirSenha(this.token, this.novaSenha).subscribe({
      next: () => {
        this.msgStep3 = "Senha redefinida! Redirecionando...";
        setTimeout(() => this.router.navigate(["/login"]), 1500);
      },
      error: () => {
        this.erroStep3 = "Erro ao redefinir senha.";
        this.loadingStep3 = false;
      },
    });
  }
}
