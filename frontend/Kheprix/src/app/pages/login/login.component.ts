import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "../../core/services/auth.service";

@Component({
  selector: "app-login",
  standalone: true,
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"],
  imports: [CommonModule, FormsModule, RouterLink],
})
export class LoginComponent {
  email = "";
  senha = "";
  erro = "";
  loading = false;
  mostrarSenha = false;

  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  onLogin() {
    if (!this.email || !this.senha) {
      this.erro = "Preencha e-mail e senha.";
      return;
    }
    this.loading = true;
    this.erro = "";
    this.auth.login({ email: this.email, senha: this.senha }).subscribe({
      next: () => this.router.navigate(["/inicio"]),
      error: () => {
        this.erro = "E-mail ou senha incorretos.";
        this.loading = false;
      },
    });
  }
}
