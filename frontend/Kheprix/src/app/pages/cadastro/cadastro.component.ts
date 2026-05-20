import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "../../core/services/auth.service";
import { extrairMensagemErro } from "../../core/utils/erro.util";

@Component({
  selector: "app-cadastro",
  standalone: true,
  templateUrl: "./cadastro.component.html",
  styleUrls: ["./cadastro.component.css"],
  imports: [CommonModule, FormsModule, RouterLink],
})
export class CadastroComponent {
  nome = "";
  email = "";
  senha = "";
  confirmarSenha = "";
  erro = "";
  sucesso = "";
  loading = false;
  mostrarSenha = false;
  mostrarSenhaC = false;

  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  // Espelha as validacoes do backend (AutocadastroDto) pra UX preventiva: usuario
  // ve o problema antes de submeter, em vez de receber 422 do servidor.
  private static readonly EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  private static readonly SENHA_MIN = 8;

  onCadastro() {
    this.erro = "";
    if (!this.nome || !this.email || !this.senha) {
      this.erro = "Preencha todos os campos obrigatórios.";
      return;
    }
    if (!CadastroComponent.EMAIL_REGEX.test(this.email)) {
      this.erro = "E-mail inválido. Verifique o formato (ex.: nome@dominio.com).";
      return;
    }
    if (this.senha.length < CadastroComponent.SENHA_MIN) {
      this.erro = `A senha deve ter pelo menos ${CadastroComponent.SENHA_MIN} caracteres.`;
      return;
    }
    if (this.senha !== this.confirmarSenha) {
      this.erro = "As senhas não coincidem.";
      return;
    }
    this.loading = true;
    this.auth
      .autocadastro({ nome: this.nome, email: this.email, senha: this.senha })
      .subscribe({
        next: () => {
          this.sucesso = "Cadastro realizado! Redirecionando...";
          setTimeout(() => this.router.navigate(["/login"]), 1500);
        },
        error: (err) => {
          this.erro = extrairMensagemErro(err, "Erro ao cadastrar. Verifique os dados.");
          this.loading = false;
        },
      });
  }
}
