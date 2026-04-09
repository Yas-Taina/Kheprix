import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AutenticacaoService } from '../../../services/autenticacao.service';

@Component({
  selector: 'app-recuperacao-senha',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recuperarsenha.html',
  styleUrls: ['./recuperarsenha.css'],
})

export class RecuperSenha {
  /* Etapa 1: solicitar token */
  email = '';
  tokenEnviado = false;

  /* Etapa 2: validar token */
  token = '';
  tokenValido = false;

  /* Etapa 3: nova senha */
  novaSenha = '';
  confirmarSenha = '';

  erro = '';
  sucesso = '';
  carregando = false;

  constructor(private auth: AutenticacaoService, private router: Router) {}

  enviarToken() {
    if (!this.email) { this.erro = 'Informe o e-mail.'; return; }
    this.carregando = true;
    this.erro = '';
    this.auth.solicitarRedefinicao({ email: this.email }).subscribe({
      next: () => { this.tokenEnviado = true; this.carregando = false; },
      error: () => { this.erro = 'E-mail não encontrado.'; this.carregando = false; },
    });
  }

  confirmarToken() {
    if (!this.token) { this.erro = 'Informe o token.'; return; }
    this.carregando = true;
    this.erro = '';
    this.auth.validarTokenRedefinicao({ token: this.token }).subscribe({
      next: (res) => {
        this.carregando = false;
        if (res.valido) { this.tokenValido = true; }
        else { this.erro = 'Token inválido ou expirado.'; }
      },
      error: () => { this.erro = 'Erro ao validar token.'; this.carregando = false; },
    });
  }

  redefinirSenha() {
    if (!this.novaSenha) { this.erro = 'Informe a nova senha.'; return; }
    if (this.novaSenha !== this.confirmarSenha) { this.erro = 'As senhas não coincidem.'; return; }
    this.carregando = true;
    this.erro = '';
    this.auth.redefinirSenha({ token: this.token, nova_senha: this.novaSenha }).subscribe({
      next: () => { this.sucesso = 'Senha redefinida! Redirecionando...'; setTimeout(() => this.router.navigate(['/login']), 2000); },
      error: () => { this.erro = 'Erro ao redefinir senha.'; this.carregando = false; },
    });
  }
}
