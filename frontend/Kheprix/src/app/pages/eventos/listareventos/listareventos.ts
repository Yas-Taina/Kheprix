import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventosAmostragemService } from '../../../services/eventos-amostragem.service';
import { EventoAmostragem } from '../../../services/modelos/evento-amostragem.model';

@Component({
  selector: 'app-eventos-amostragem',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './eventos-amostragem.component.html',
  styleUrls: ['./eventos-amostragem.component.css'],
})
export class EventosAmostragemComponent implements OnInit {
  estudoId!: number;
  campanhaId!: number;
  unidadeId!: number;
  nomeUnidade = '';
  eventos: EventoAmostragem[] = [];
  carregando = true;
  erro = '';
  toast = '';

  /* Formulário inline para novo evento */
  mostrarFormNovoEvento = false;
  novoInicio = '';
  novoFim = '';
  novoEsforco = '';
  erroForm = '';
  salvando = false;

  /* Detalhe inline */
  eventoDetalhe: EventoAmostragem | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventosService: EventosAmostragemService,
  ) {}

  ngOnInit() {
    this.estudoId   = Number(this.route.snapshot.paramMap.get('estudoId'));
    this.campanhaId = Number(this.route.snapshot.paramMap.get('campanhaId'));
    this.unidadeId  = Number(this.route.snapshot.paramMap.get('unidadeId'));
    this.nomeUnidade = history.state?.nomeUnidade || `Unidade Amostral ${this.unidadeId}`;
    this.carregar();
  }

  carregar() {
    this.eventosService.listar(this.estudoId, this.campanhaId, this.unidadeId).subscribe({
      next: (data) => { this.eventos = data; this.carregando = false; },
      error: () => { this.erro = 'Erro ao carregar eventos.'; this.carregando = false; },
    });
  }

  verDetalhes(e: EventoAmostragem, event: Event) {
    event.stopPropagation();
    this.eventoDetalhe = this.eventoDetalhe?.id === e.id ? null : e;
  }

  toggleFormNovoEvento() {
    this.mostrarFormNovoEvento = !this.mostrarFormNovoEvento;
    this.erroForm = '';
    this.novoInicio = '';
    this.novoFim = '';
    this.novoEsforco = '';
  }

  salvarEvento() {
    if (!this.novoInicio) { this.erroForm = 'Informe o horário de início.'; return; }
    this.salvando = true;
    this.erroForm = '';
    this.eventosService.criar(this.estudoId, this.campanhaId, this.unidadeId, {
      horario_inicio: this.novoInicio,
      horario_fim:    this.novoFim    || undefined,
      esforco_real:   this.novoEsforco || undefined,
    }).subscribe({
      next: () => {
        this.mostrarFormNovoEvento = false;
        this.salvando = false;
        this.carregar();
      },
      error: () => { this.erroForm = 'Erro ao salvar evento.'; this.salvando = false; },
    });
  }

  deletar(e: EventoAmostragem, event: Event) {
    event.stopPropagation();
    if (!confirm(`Deletar evento #${e.id}?`)) return;
    this.eventosService.deletar(this.estudoId, this.campanhaId, this.unidadeId, e.id).subscribe({
      next: () => { this.showToast('Evento removido.'); this.carregar(); },
      error: () => this.showToast('Erro ao deletar.'),
    });
  }

  formatDataHora(dt: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('pt-BR');
  }

  formatData(dt: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleDateString('pt-BR');
  }

  showToast(msg: string) { this.toast = msg; setTimeout(() => this.toast = '', 3000); }

  voltar() {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades']);
  }
}
