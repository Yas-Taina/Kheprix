import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UnidadesAmostraisService } from '../../../services/unidades-amostrais.service';
import { VariaveisService } from '../../../services/variaveis.service';
import { Variavel } from '../../../services/modelos/variavel.model';

interface ValorVar { variavel: Variavel; valor: string; }

@Component({
  selector: 'app-nova-unidade-amostral',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cadastrarunidades.html',
  styleUrls: ['./cadastrarunidades.css'],
})
export class CadastrarUnidades implements OnInit {
  estudoId!: number;
  campanhaId!: number;

  nome = '';
  latitudeStr = '';
  longitudeStr = '';
  raio: number | null = null;
  metodoColeta = '';
  esforcoAmostral = '';
  variaveis: ValorVar[] = [];

  erro = '';
  carregando = false;
  gpsCarregando = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private unidadesService: UnidadesAmostraisService,
    private variaveisService: VariaveisService,
  ) {}

  ngOnInit() {
    this.estudoId   = Number(this.route.snapshot.paramMap.get('estudoId'));
    this.campanhaId = Number(this.route.snapshot.paramMap.get('campanhaId'));
    this.variaveisService.listar(this.estudoId, 'unidade').subscribe({
      next: (vars) => { this.variaveis = vars.map(v => ({ variavel: v, valor: '' })); },
    });
  }

  /* ── GPS: preenche latitude e longitude no formato −00°00'00" ── */
  obterLocalizacao() {
    if (!navigator.geolocation) { this.erro = 'Geolocalização não suportada.'; return; }
    this.gpsCarregando = true;
    this.erro = '';
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.latitudeStr  = this.decimalParaDMS(pos.coords.latitude);
        this.longitudeStr = this.decimalParaDMS(pos.coords.longitude);
        this.gpsCarregando = false;
      },
      () => { this.erro = 'Não foi possível obter a localização.'; this.gpsCarregando = false; },
      { enableHighAccuracy: true },
    );
  }

  /** Converte decimal para −DD°MM'SS" */
  decimalParaDMS(decimal: number): string {
    const sign = decimal < 0 ? '-' : '';
    const abs  = Math.abs(decimal);
    const deg  = Math.floor(abs);
    const minF = (abs - deg) * 60;
    const min  = Math.floor(minF);
    const sec  = Math.round((minF - min) * 60);
    return `${sign}${deg}°${String(min).padStart(2, '0')}'${String(sec).padStart(2, '0')}"`;
  }

  /** Converte DMS de volta para decimal para enviar à API */
  dmsParaDecimal(dms: string): number {
    const clean = dms.trim();
    const neg   = clean.startsWith('-');
    const num   = clean.replace('-', '').replace('°', ' ').replace("'", ' ').replace('"', '').trim();
    const parts = num.split(/\s+/);
    const deg   = parseFloat(parts[0] || '0');
    const min   = parseFloat(parts[1] || '0');
    const sec   = parseFloat(parts[2] || '0');
    const result = deg + min / 60 + sec / 3600;
    return neg ? -result : result;
  }

  confirmar() {
    if (!this.nome || !this.latitudeStr || !this.longitudeStr) {
      this.erro = 'Informe o nome, latitude e longitude.';
      return;
    }
    this.carregando = true;
    this.erro = '';
    this.unidadesService.criar(this.estudoId, this.campanhaId, {
      nome: this.nome,
      latitude:  this.dmsParaDecimal(this.latitudeStr),
      longitude: this.dmsParaDecimal(this.longitudeStr),
      raio:            this.raio ?? undefined,
      metodo_coleta:   this.metodoColeta  || undefined,
      esforco_amostral: this.esforcoAmostral || undefined,
    }).subscribe({
      next: () => this.router.navigate([
        '/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades',
      ]),
      error: () => { this.erro = 'Erro ao salvar unidade amostral.'; this.carregando = false; },
    });
  }

  voltar() {
    this.router.navigate(['/estudos', this.estudoId, 'campanhas', this.campanhaId, 'unidades']);
  }
}
