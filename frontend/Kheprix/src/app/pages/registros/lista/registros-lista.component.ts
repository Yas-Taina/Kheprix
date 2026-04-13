import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RegistroOcorrenciaService } from '../../../core/services/registro-ocorrencia.service';
import { EventoAmostragemService } from '../../../core/services/evento-amostragem.service';
import { EspecieService } from '../../../core/services/especie.service';
import { RegistroOcorrencia, EventoAmostragem, Especie } from '../../../models';
import { UtilService } from '../../../core/services/util.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-registros-lista',
  standalone: true,
  templateUrl: './registros-lista.component.html',
  styleUrls: ['./registros-lista.component.css'],
  imports: [CommonModule, FormsModule],
})
export class RegistrosListaComponent implements OnInit {
  registros: RegistroOcorrencia[] = [];
  registrosFiltrados: RegistroOcorrencia[] = [];
  especies: Especie[] = [];
  eventoDetalhe: EventoAmostragem | null = null;
  estudoId!: number; campanhaId!: number; unidadeId!: number; eventoId!: number;
  loading = true; showDetalhes = false; busca = '';
  apiUrl = environment.apiUrl;
  placeholderImg = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="52" height="52"%3E%3Ccircle cx="26" cy="26" r="25" fill="%23D4CDBA"/%3E%3Ctext x="50%25" y="55%25" text-anchor="middle" font-size="9" fill="%238A7D6E"%3EFoto%3C/text%3E%3C/svg%3E';

  constructor(
    private registroService: RegistroOcorrenciaService,
    private eventoService: EventoAmostragemService,
    private especieService: EspecieService,
    public router: Router,
    private route: ActivatedRoute,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params['estudo_id'];
    this.campanhaId = +this.route.snapshot.params['campanha_id'];
    this.unidadeId = +this.route.snapshot.params['unidade_id'];
    this.eventoId = +this.route.snapshot.params['evento_id'];

    this.registroService.listar(this.estudoId, this.campanhaId, this.unidadeId, this.eventoId).subscribe({
      next: r => { this.registros = r; this.registrosFiltrados = r; this.loading = false; },
      error: () => this.loading = false,
    });
    this.eventoService.buscar(this.estudoId, this.campanhaId, this.unidadeId, this.eventoId).subscribe(ev => this.eventoDetalhe = ev);
    this.especieService.listar(this.estudoId).subscribe(e => this.especies = e);
  }

  filtrar() {
    if (!this.busca.trim()) { this.registrosFiltrados = this.registros; return; }
    const termo = this.busca.toLowerCase();
    this.registrosFiltrados = this.registros.filter(r => this.getNomeEspecie(r.especie_id).toLowerCase().includes(termo));
  }

  getNomeEspecie(id: number): string {
    const e = this.especies.find(x => x.id === id);
    return e ? `${e.genero} ${e.especie}` : `Espécie #${id}`;
  }

  toggleDetalhes() { this.showDetalhes = !this.showDetalhes; }

  verDetalhe(r: RegistroOcorrencia) {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades', this.unidadeId, 'eventos', this.eventoId, 'registros', r.id]);
  }

  novoRegistro() {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades', this.unidadeId, 'eventos', this.eventoId, 'registros', 'novo']);
  }

  editarEvento() {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades', this.unidadeId, 'eventos', this.eventoId, 'editar']);
  }

  voltar() {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades', this.unidadeId, 'eventos']);
  }
}
