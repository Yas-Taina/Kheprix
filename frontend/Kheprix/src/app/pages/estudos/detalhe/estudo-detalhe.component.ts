import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { EstudoService } from '../../../core/services/estudo.service';
import { Estudo, TipoAgrupamento } from '../../../models';
import { UtilService } from '../../../core/services/util.service';

@Component({
  selector: 'app-estudo-detalhe',
  standalone: true,
  templateUrl: './estudo-detalhe.component.html',
  styleUrls: ['./estudo-detalhe.component.css'],
  imports: [CommonModule, FormsModule],
})
export class EstudoDetalheComponent implements OnInit {
  estudo: Estudo | null = null;
  loading = true;
  estudoId!: number;
  showExportar = false;
  agrupamento: TipoAgrupamento = 'registro_ocorrencia';
  exportLoading = false;
  exportMsg = '';

  constructor(
    private estudoService: EstudoService,
    private route: ActivatedRoute,
    public router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params['estudo_id'];
    // Buscamos da lista pois não há GET /estudos/:id
    this.estudoService.listar().subscribe({
      next: (lista) => {
        this.estudo = lista.find(e => e.id === this.estudoId) ?? null;
        this.loading = false;
      },
      error: () => this.loading = false,
    });
  }

  irParaCampanhas() { this.router.navigate(['/estudos', this.estudoId, 'campanhas']); }
  irParaNovaEspecie() { this.router.navigate(['/estudos', this.estudoId, 'especies', 'novo']); }
  irParaEspecies() { this.router.navigate(['/estudos', this.estudoId, 'especies']); }
  irParaEditar() { this.router.navigate(['/estudos', this.estudoId, 'editar']); }
  abrirExportar() { this.showExportar = true; this.exportMsg = ''; }
  fecharExportar() { this.showExportar = false; }

  exportar() {
    this.exportLoading = true;
    this.estudoService.exportarDados(this.estudoId, this.agrupamento, 'csv').subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `dados_estudo_${this.estudoId}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        this.exportLoading = false;
        this.exportMsg = 'Arquivo baixado!';
      },
      error: () => { this.exportLoading = false; },
    });
  }
}
