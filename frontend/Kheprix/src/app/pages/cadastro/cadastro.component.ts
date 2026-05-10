import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "../../core/services/auth.service";

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

  onCadastro() {
    if (!this.nome || !this.email || !this.senha) {
      this.erro = "Preencha todos os campos obrigatórios.";
      return;
    }
    if (this.senha !== this.confirmarSenha) {
      this.erro = "As senhas não coincidem.";
      return;
    }
    this.loading = true;
    this.erro = "";
    this.auth
      .autocadastro({ nome: this.nome, email: this.email, senha: this.senha })
      .subscribe({
        next: () => {
          this.sucesso = "Cadastro realizado! Redirecionando...";
          setTimeout(() => this.router.navigate(["/login"]), 1500);
        },
        error: () => {
          this.erro = "Erro ao cadastrar. Verifique os dados.";
          this.loading = false;
        },
      });
  }
}
