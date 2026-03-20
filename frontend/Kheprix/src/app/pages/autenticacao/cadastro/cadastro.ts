import { Component, OnInit, inject } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from "@angular/forms";
import { RouterModule, Router, RouterLink } from "@angular/router";
import { CommonModule } from "@angular/common";
import { NgxMaskDirective } from "ngx-mask";
import { AuthService } from "../../../services/auth.service";

function senhasIguais(control: AbstractControl): ValidationErrors | null {
  const senha = control.get('senha')?.value;
  const confirmar = control.get('confirmar_senha')?.value;
  return senha && confirmar && senha !== confirmar ? { senhasDivergentes: true } : null;
}

@Component({
  selector: 'app-cadastro',
  imports: [RouterModule, RouterLink, CommonModule, ReactiveFormsModule, NgxMaskDirective],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css',
})

export class Cadastro {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = false;
  error = '';

  form = this.fb.group({
    nome: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
    confirmar_senha: ['', Validators.required],
  }, { validators: senhasIguais });

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    const { confirmar_senha, ...payload } = this.form.getRawValue() as any;
    this.authService.autocadastro(payload).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (err) => {
        this.error = err?.error?.mensagem ?? 'Erro ao criar conta.';
        this.loading = false;
      },
    });
  }
}
