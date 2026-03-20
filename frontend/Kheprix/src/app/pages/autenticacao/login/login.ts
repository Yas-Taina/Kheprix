import { Component, inject } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterModule, Router, RouterLink } from "@angular/router";
import { CommonModule } from "@angular/common";
import { NgxMaskDirective } from "ngx-mask";
import { AuthService } from "../../../services/auth.service";

@Component({
  selector: 'app-login',
  imports: [RouterModule, RouterLink, CommonModule, ReactiveFormsModule, NgxMaskDirective],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = false;
  error = '';

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.authService.login(this.form.getRawValue() as any).subscribe({
      next: () => this.router.navigate(['/estudos']),
      error: (err) => {
        this.error = err?.error?.mensagem ?? 'Erro ao fazer login.';
        this.loading = false;
      },
    });
  }
}
