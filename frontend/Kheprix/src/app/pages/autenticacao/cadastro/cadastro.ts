import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AutenticacaoService } from '../../../services/autenticacao.service';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cadastro.html',
  styleUrls: ['./cadastro.css'],
})
export class Cadastro {
  nome = '';
  email = '';
  senha = '';
  confirmarSenha = '';
  erro = '';
  carregando = false;

  constructor(private auth: AutenticacaoService, private router: Router) {}

  confirmar() {
    if (!this.nome || !this.email || !this.senha) {
      this.erro = 'Preencha todos os campos.'; return;
    }
    if (this.senha !== this.confirmarSenha) {
      this.erro = 'As senhas não coincidem.'; return;
    }
    this.carregando = true;
    this.erro = '';
    this.auth.autocadastro({ nome: this.nome, email: this.email, senha: this.senha }).subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => { this.erro = 'Erro ao cadastrar. Tente novamente.'; this.carregando = false; },
    });
  }

  irParaLogin() { this.router.navigate(['/login']); }
}
