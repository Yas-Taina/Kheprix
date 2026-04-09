import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EspeciesService } from '../../../services/especies.service';
import { Especie } from '../../../services/modelos/especie.model';


@Component({
  selector: 'app-especie-detalhe',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './detalharespecie.html',
  styleUrls: ['./detalharespecie.css'],
})
export class DetalharEspecie implements OnInit {
  estudoId!: number;
  especieId!: number;
  especie: Especie | null = null;
  carregando = true;
  erro = '';
  toast = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private especiesService: EspeciesService,
  ) {}

  ngOnInit() {
    this.estudoId  = Number(this.route.snapshot.paramMap.get('estudoId'));
    this.especieId = Number(this.route.snapshot.paramMap.get('id'));
    this.carregar();
  }

  carregar() {
    this.especiesService.buscarPorId(this.estudoId, this.especieId).subscribe({
      next: (e) => { this.especie = e; this.carregando = false; },
      error: () => { this.erro = 'Erro ao carregar espécie.'; this.carregando = false; },
    });
  }

  editar() {
    this.router.navigate(['/estudos', this.estudoId, 'especies', this.especieId, 'editar']);
  }

  deletar() {
    if (!confirm('Deletar esta espécie?')) return;
    this.especiesService.deletar(this.estudoId, this.especieId).subscribe({
      next: () => this.router.navigate(['/estudos', this.estudoId, 'especies']),
      error: () => this.showToast('Erro ao deletar.'),
    });
  }

  voltar() { this.router.navigate(['/estudos', this.estudoId, 'especies']); }

  statusDotClass(status: string) {
    if (!status) return '';
    const s = status.toLowerCase();
    if (s.includes('ameaça') || s.includes('extint')) return 'ameacada';
    if (s.includes('vulnerável') || s.includes('vulneravel')) return 'vulneravel';
    return 'ok';
  }

  showToast(msg: string) { this.toast = msg; setTimeout(() => this.toast = '', 3000); }
}
