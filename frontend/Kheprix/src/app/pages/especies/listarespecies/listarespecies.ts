import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EspeciesService } from '../../../services/especies.service';
import { Especie } from '../../../services/modelos/especie.model';

@Component({
  selector: 'app-especies',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './especies.component.html',
  styleUrls: ['./especies.component.css'],
})
export class ListarEspecies implements OnInit {
  estudoId!: number;
  nomeEstudo = '';
  especies: Especie[] = [];
  filtradas: Especie[] = [];
  busca = '';
  carregando = true;
  erro = '';
  toast = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private especiesService: EspeciesService,
  ) {}

  ngOnInit() {
    this.estudoId = Number(this.route.snapshot.paramMap.get('estudoId'));
    this.nomeEstudo = history.state?.nomeEstudo || 'Estudo';
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.especiesService.listar(this.estudoId).subscribe({
      next: (data) => { this.especies = data; this.filtradas = data; this.carregando = false; },
      error: () => { this.erro = 'Erro ao carregar espécies.'; this.carregando = false; },
    });
  }

  pesquisar() {
    const q = this.busca.toLowerCase();
    this.filtradas = this.especies.filter(e =>
      `${e.genero} ${e.especie}`.toLowerCase().includes(q) ||
      (e.nome_popular || '').toLowerCase().includes(q)
    );
  }

  abrirDetalhe(e: Especie) {
    this.router.navigate(['/estudos', this.estudoId, 'especies', e.id]);
  }

  novaEspecie() {
    this.router.navigate(['/estudos', this.estudoId, 'especies', 'novo']);
  }

  voltar() { this.router.navigate(['/estudos']); }

  formatNomeCientifico(e: Especie) { return `${e.genero} ${e.especie}`; }

  showToast(msg: string) { this.toast = msg; setTimeout(() => this.toast = '', 3000); }
}
