import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CampanhaService } from '../../../core/services/campanha.service';
import { VariavelService } from '../../../core/services/variavel.service';
import { Variavel, ValorVariavel } from '../../../models';

@Component({
  selector: 'app-campanha-novo',
  standalone: true,
  templateUrl: './campanha-novo.component.html',
  styleUrls: ['./campanha-novo.component.css'],
  imports: [CommonModule, FormsModule],
})
export class CampanhaNovoComponent implements OnInit {
  estudoId!: number; campanhaId: number | null = null; isEdit = false;
  nome = ''; dataInicio = ''; descricao = '';
  variaveis: Variavel[] = []; valoresVars: ValorVariavel[] = [];
  loading = false; erro = '';

  constructor(private campanhaService: CampanhaService, private variavelService: VariavelService,
    public router: Router, private route: ActivatedRoute) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params['estudo_id'];
    this.campanhaId = this.route.snapshot.params['campanha_id'] ? +this.route.snapshot.params['campanha_id'] : null;
    this.isEdit = !!this.campanhaId;
    this.variavelService.listar(this.estudoId, 'campanha').subscribe(vars => {
      this.variaveis = vars;
      this.valoresVars = vars.map(v => ({ variavel_id: v.id, valor: '' }));
    });
    if (this.isEdit && this.campanhaId) {
      this.campanhaService.buscar(this.estudoId, this.campanhaId).subscribe(c => {
        this.nome = c.nome; this.dataInicio = c.data_inicio; this.descricao = c.descricao ?? '';
      });
    }
  }

  salvar() {
    if (!this.nome || !this.dataInicio) { this.erro = 'Nome e data de início são obrigatórios.'; return; }
    this.loading = true; this.erro = '';
    const payload = { nome: this.nome, data_inicio: this.dataInicio, descricao: this.descricao || undefined, valores_variaveis: this.valoresVars.filter(v => v.valor !== '' && v.valor !== null && v.valor !== undefined) };
    const obs = this.isEdit && this.campanhaId
      ? this.campanhaService.atualizar(this.estudoId, this.campanhaId, payload)
      : this.campanhaService.criar(this.estudoId, payload);
    obs.subscribe({
      next: () => this.voltar(),
      error: () => { this.erro = 'Erro ao salvar.'; this.loading = false; },
    });
  }

  voltar() { this.router.navigate(['/estudos', this.estudoId, 'campanhas']); }
}
