import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EstudosService } from '../../../services/estudos.service';

interface VariavelForm {
  nome: string;
  nivel_aplicacao: string;
  tipo_dado: string;
  metrica: string;
}

@Component({
  selector: 'app-novo-estudo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cadastrarestudos.html',
  styleUrls: ['./cadastrarestudos.css'],
})
export class CadastrarEstudo {
  nome = '';
  observacoes = '';
  variaveis: VariavelForm[] = [this.novaVariavel()];
  erro = '';
  carregando = false;

  niveisAplicacao = ['campanha', 'unidade', 'evento', 'registro'];
  tiposDado = ['numerico', 'tipado', 'data'];

  constructor(private estudosService: EstudosService, private router: Router) {}

  novaVariavel(): VariavelForm {
    return { nome: '', nivel_aplicacao: 'unidade', tipo_dado: 'numerico', metrica: '' };
  }

  adicionarVariavel() { this.variaveis.push(this.novaVariavel()); }

  removerVariavel(i: number) {
    if (this.variaveis.length > 1) this.variaveis.splice(i, 1);
  }

  confirmar() {
    if (!this.nome) { this.erro = 'Informe o nome do estudo.'; return; }
    const varFiltradas = this.variaveis.filter(v => v.nome.trim());
    if (varFiltradas.length === 0) { this.erro = 'Adicione ao menos uma variável.'; return; }
    this.carregando = true;
    this.erro = '';
    this.estudosService.criar({
      nome: this.nome,
      observacoes: this.observacoes || undefined,
      variaveis: varFiltradas.map(v => ({
        nome: v.nome,
        nivel_aplicacao: v.nivel_aplicacao,
        tipo_dado: v.tipo_dado,
        metrica: v.metrica || undefined,
      })),
    }).subscribe({
      next: () => this.router.navigate(['/estudos']),
      error: () => { this.erro = 'Erro ao criar estudo.'; this.carregando = false; },
    });
  }

  voltar() { this.router.navigate(['/estudos']); }

  labelNivel(n: string) {
    const map: Record<string,string> = {
      campanha: 'Campanha', unidade: 'Unidade Amostral',
      evento: 'Evento', registro: 'Registro',
    };
    return map[n] || n;
  }
}
