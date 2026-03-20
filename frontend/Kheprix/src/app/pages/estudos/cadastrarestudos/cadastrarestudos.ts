import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormArray,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EstudosService } from '../../../services/estudos.service';

const NIVEIS_APLICACAO = ['campanha', 'unidade', 'evento', 'registro'] as const;
const TIPOS_DADO = ['qualitativo', 'quantitativo', 'data'] as const;

@Component({
  selector: 'app-cadastrarestudos',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './cadastrarestudos.html',
  styleUrl: './cadastrarestudos.css',
})
export class Cadastrarestudos implements OnInit {
 private fb = inject(FormBuilder);
  private estudosService = inject(EstudosService);
  private router = inject(Router);
 
  readonly niveisAplicacao = NIVEIS_APLICACAO;
  readonly tiposDado = TIPOS_DADO;
 
  loading = false;
  error = '';
 
  form = this.fb.group({
    nome: ['', Validators.required],
    observacoes: [''],
    variaveis: this.fb.array([]),
  });
 
  get variaveis(): FormArray {
    return this.form.get('variaveis') as FormArray;
  }
 
  ngOnInit(): void {
    this.adicionarVariavel(); 
  }
 
  criarVariavelGroup() {
    return this.fb.group({
      nome: ['', Validators.required],
      nivel_aplicacao: ['', Validators.required],
      tipo_dado: ['', Validators.required],
      metrica: [''],
    });
  }
 
  adicionarVariavel(): void {
    this.variaveis.push(this.criarVariavelGroup());
  }
 
  removerVariavel(index: number): void {
    if (this.variaveis.length > 1) {
      this.variaveis.removeAt(index);
    }
  }
 
  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.estudosService.criar(this.form.getRawValue() as any).subscribe({
      next: () => this.router.navigate(['/estudos']),
      error: (err) => {
        this.error = err?.error?.mensagem ?? 'Erro ao salvar estudo.';
        this.loading = false;
      },
    });
  }
}
