import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CampanhasService } from '../../../services/campanhas.service';
import { Campanha } from '../../../services/modelos/campanha.model';

@Component({
  selector: 'app-campanhas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './listarcampanhas.html',
  styleUrls: ['./listarcampanhas.css'],
})
export class ListarCampanhas implements OnInit {
  estudoId!: number;
  nomeEstudo = '';
  campanhas: Campanha[] = [];
  carregando = true;
  erro = '';
  toast = '';

  /* Detalhe inline */
  campanhaDetalhe: Campanha | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private campanhasService: CampanhasService,
  ) {}

  ngOnInit() {
    this.estudoId  = Number(this.route.snapshot.paramMap.get('estudoId'));
    this.nomeEstudo = history.state?.nomeEstudo || 'Estudo';
    this.carregar();
  }

  carregar() {
    this.campanhasService.listar(this.estudoId).subscribe({
      next: (data) => { this.campanhas = data; this.carregando = false; },
      error: () => { this.erro = 'Erro ao carregar campanhas.'; this.carregando = false; },
    });
  }

  novaCampanha() { this.router.navigate(['/estudos', this.estudoId, 'campanhas', 'nova']); }

  abrirUnidades(c: Campanha) {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', c.id, 'unidades']);
  }

  verDetalhes(c: Campanha, event: Event) {
    event.stopPropagation();
    this.campanhaDetalhe = this.campanhaDetalhe?.id === c.id ? null : c;
  }

  deletar(c: Campanha, event: Event) {
    event.stopPropagation();
    if (!confirm(`Deletar campanha #${c.id}?`)) return;
    this.campanhasService.deletar(this.estudoId, c.id).subscribe({
      next: () => { this.showToast('Campanha removida.'); this.carregar(); },
      error: () => this.showToast('Erro ao deletar.'),
    });
  }

  formatData(d: string) { return d ? new Date(d).toLocaleDateString('pt-BR') : '—'; }

  showToast(msg: string) { this.toast = msg; setTimeout(() => this.toast = '', 3000); }

  voltar() { this.router.navigate(['/estudos']); }
}
