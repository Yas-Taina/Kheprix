import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EstudoService } from '../../../core/services/estudo.service';
import { Estudo } from '../../../models';
import { UtilService } from '../../../core/services/util.service';

@Component({
  selector: 'app-estudos-lista',
  standalone: true,
  templateUrl: './estudos-lista.component.html',
  styleUrls: ['./estudos-lista.component.css'],
  imports: [CommonModule, FormsModule],
})
export class EstudosListaComponent implements OnInit {
  estudos: Estudo[] = [];
  loading = true;
  mostrarFiltros = false;
  tipoBusca = 'nome';
  termoBusca = '';
  filtros: Record<string, string> = {};

  constructor(
    private estudoService: EstudoService,
    public router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() { this.carregar(); }

  carregar() {
    this.loading = true;
    this.estudoService.listar(this.filtros as any).subscribe({
      next: (e) => { this.estudos = e; this.loading = false; },
      error: () => this.loading = false,
    });
  }

  buscar() {
    const f: Record<string, string> = {};
    if (this.tipoBusca === 'nome' && this.termoBusca) f['nome'] = this.termoBusca;
    if (this.filtros['criado_a_partir_de']) f['criado_a_partir_de'] = this.filtros['criado_a_partir_de'];
    if (this.filtros['criado_ate']) f['criado_ate'] = this.filtros['criado_ate'];
    if (this.filtros['atualizado_a_partir_de']) f['atualizado_a_partir_de'] = this.filtros['atualizado_a_partir_de'];
    if (this.filtros['atualizado_ate']) f['atualizado_ate'] = this.filtros['atualizado_ate'];
    this.filtros = f;
    this.carregar();
  }

  isProprietario(e: Estudo): boolean {
    return e.perfil === 'proprietario';
  }

  verDetalhe(e: Estudo) { this.router.navigate(['/estudos', e.id]); }
  verColaboradores(e: Estudo) { this.router.navigate(['/estudos', e.id, 'colaboradores']); }

  deletar(e: Estudo) {
    if (!confirm(`Excluir o estudo "${e.nome}"?`)) return;
    this.estudoService.deletar(e.id).subscribe({
      next: () => this.estudos = this.estudos.filter(x => x.id !== e.id),
      error: () => alert('Erro ao excluir.'),
    });
  }
}
