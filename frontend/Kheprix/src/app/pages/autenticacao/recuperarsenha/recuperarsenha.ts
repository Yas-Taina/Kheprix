import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

function senhasIguais(control: AbstractControl): ValidationErrors | null {
  const nova = control.get('nova_senha')?.value;
  const confirmar = control.get('confirmar_senha')?.value;
  return nova && confirmar && nova !== confirmar ? { senhasDivergentes: true } : null;
}

type Etapa = 'email' | 'token' | 'nova-senha';

@Component({
  selector: 'app-recuperarsenha',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './recuperarsenha.html',
  styleUrl: './recuperarsenha.css',
})
export class Recuperarsenha {
private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  etapa: Etapa = 'email';
  loading = false;
  error = '';
  sucesso = '';

  emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  tokenForm = this.fb.group({
    token: ['', Validators.required],
  });

  senhaForm = this.fb.group({
    nova_senha: ['', [Validators.required, Validators.minLength(8)]],
    confirmar_senha: ['', Validators.required],
  }, { validators: senhasIguais });


  private tokenValue = '';

  enviarEmail(): void {
    if (this.emailForm.invalid) return;
    this.loading = true;
    this.error = '';
    this.authService.solicitarRedefinicao(this.emailForm.getRawValue() as any).subscribe({
      next: () => {
        this.etapa = 'token';
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.mensagem ?? 'Erro ao enviar e-mail.';
        this.loading = false;
      },
    });
  }

  validarToken(): void {
    if (this.tokenForm.invalid) return;
    this.loading = true;
    this.error = '';
    const { token } = this.tokenForm.getRawValue() as any;
    this.authService.validarTokenRedefinicao({ token }).subscribe({
      next: (res) => {
        if (res.valido) {
          this.tokenValue = token;
          this.etapa = 'nova-senha';
        } else {
          this.error = 'Token inválido ou expirado.';
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.mensagem ?? 'Erro ao validar token.';
        this.loading = false;
      },
    });
  }

  redefinirSenha(): void {
    if (this.senhaForm.invalid) return;
    this.loading = true;
    this.error = '';
    const { nova_senha } = this.senhaForm.getRawValue() as any;
    this.authService.redefinirSenha({ token: this.tokenValue, nova_senha }).subscribe({
      next: () => {
        this.sucesso = 'Senha alterada com sucesso!';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.error = err?.error?.mensagem ?? 'Erro ao redefinir senha.';
        this.loading = false;
      },
    });
  }
}
