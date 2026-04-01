import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AutenticacaoService } from '../../../services/autenticacao.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class Login {
  email = '';
  senha = '';
  erro = '';
  carregando = false;

  constructor(private auth: AutenticacaoService, private router: Router) {}

  confirmar() {
    if (!this.email || !this.senha) { this.erro = 'Preencha todos os campos.'; return; }
    this.carregando = true;
    this.erro = '';
    this.auth.login({ email: this.email, senha: this.senha }).subscribe({
      next: () => this.router.navigate(['/estudos']),
      error: () => { this.erro = 'E-mail ou senha incorretos.'; this.carregando = false; },
    });
  }

  irParaCadastro()         { this.router.navigate(['/cadastro']); }
  irParaRecuperacao()      { this.router.navigate(['/recuperar-senha']); }
}
