import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UnidadesAmostraisService } from '../../../services/unidades-amostrais.service';
import { UnidadeAmostral } from '../../../services/modelos/unidade-amostral.model';

@Component({
  selector: 'app-unidades-amostrais',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './listarunidades.html',
  styleUrls: ['./listarunidades.css'],
})
export class ListarUnidades implements OnInit {
  estudoId!: number;
  campanhaId!: number;
  nomeCampanha = '';
  unidades: UnidadeAmostral[] = [];
  carregando = true;
  erro = '';
  toast = '';
  unidadeDetalhe: UnidadeAmostral | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private unidadesService: UnidadesAmostraisService,
  ) {}

  ngOnInit() {
    this.estudoId   = Number(this.route.snapshot.paramMap.get('estudoId'));
    this.campanhaId = Number(this.route.snapshot.paramMap.get('campanhaId'));
    this.nomeCampanha = history.state?.nomeCampanha || `Campanha de Coleta ${this.campanhaId}`;
    this.carregar();
  }

  carregar() {
    this.unidadesService.listar(this.estudoId, this.campanhaId).subscribe({
      next: (data) => { this.unidades = data; this.carregando = false; },
      error: () => { this.erro = 'Erro ao carregar unidades.'; this.carregando = false; },
    });
  }

  novaUnidade() {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades', 'nova']);
  }

  abrirEventos(u: UnidadeAmostral) {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades', u.id, 'eventos']);
  }

  verDetalhes(u: UnidadeAmostral, event: Event) {
    event.stopPropagation();
    this.unidadeDetalhe = this.unidadeDetalhe?.id === u.id ? null : u;
  }

  deletar(u: UnidadeAmostral, event: Event) {
    event.stopPropagation();
    if (!confirm(`Deletar unidade #${u.id}?`)) return;
    this.unidadesService.deletar(this.estudoId, this.campanhaId, u.id).subscribe({
      next: () => { this.showToast('Unidade removida.'); this.carregar(); },
      error: () => this.showToast('Erro ao deletar.'),
    });
  }

  formatCoord(v: number) {
    if (v == null) return '—';
    const abs = Math.abs(v);
    const deg = Math.floor(abs);
    const minFrac = (abs - deg) * 60;
    const min = Math.floor(minFrac);
    const sec = Math.round((minFrac - min) * 60);
    const sign = v < 0 ? '-' : '';
    return `${sign}${deg}°${String(min).padStart(2,'0')}'${String(sec).padStart(2,'0')}"`;
  }

  showToast(msg: string) { this.toast = msg; setTimeout(() => this.toast = '', 3000); }
  voltar() { this.router.navigate(['/estudos', this.estudoId, 'campanhas']); }
}
