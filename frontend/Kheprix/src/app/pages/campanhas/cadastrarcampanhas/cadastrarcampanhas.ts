import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CampanhasService } from '../../../services/campanhas.service';
import { VariaveisService } from '../../../services/variaveis.service';
import { Variavel } from '../../../services/modelos/variavel.model';


interface ValorVar { variavel: Variavel; valor: string; }

@Component({
  selector: 'app-nova-campanha',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cadastrarcampanhas.html',
  styleUrls: ['./cadastrarcampanhas.css'],
})
export class CadastrarCampanha implements OnInit {
  estudoId!: number;
  nome = '';
  dataInicio = '';
  dataFim = '';
  descricao = '';
  variaveis: ValorVar[] = [];
  erro = '';
  carregando = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private campanhasService: CampanhasService,
    private variaveisService: VariaveisService,
  ) {}

  ngOnInit() {
    this.estudoId = Number(this.route.snapshot.paramMap.get('estudoId'));
    this.variaveisService.listar(this.estudoId, 'campanha').subscribe({
      next: (vars) => { this.variaveis = vars.map(v => ({ variavel: v, valor: '' })); },
    });
  }

  confirmar() {
    if (!this.nome || !this.dataInicio) { this.erro = 'Informe o nome e a data de início.'; return; }
    this.carregando = true;
    this.erro = '';
    this.campanhasService.criar(this.estudoId, {
      nome: this.nome,
      data_inicio: this.dataInicio,
      data_fim: this.dataFim || undefined,
      descricao: this.descricao || undefined,
      valores_variaveis: this.variaveis
        .filter(v => v.valor.trim())
        .map(v => ({ variavel_id: v.variavel.id, valor: v.valor })),
    }).subscribe({
      next: () => this.router.navigate(['/estudos', this.estudoId, 'campanhas']),
      error: () => { this.erro = 'Erro ao criar campanha.'; this.carregando = false; },
    });
  }

  voltar() { this.router.navigate(['/estudos', this.estudoId, 'campanhas']); }
}
