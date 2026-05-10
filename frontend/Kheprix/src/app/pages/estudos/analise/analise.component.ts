import { Component, OnInit, inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { trigger, transition, style, animate } from "@angular/animations";
import { forkJoin, of } from "rxjs";
import { catchError } from "rxjs/operators";

import { AnaliseService } from "../../../core/services/analise.service";
import { VariavelService } from "../../../core/services/variavel.service";
import { CampanhaService } from "../../../core/services/campanha.service";
import { UnidadeAmostralService } from "../../../core/services/unidade-amostral.service";
import { UtilService } from "../../../core/services/util.service";
import { ActivatedRoute } from "@angular/router";

import {
  CATALOGO_ANALISES,
  NOME_CATEGORIA,
  CatalogoAnalise,
  CategoriaAnalise,
  ExecutarAnaliseRequest,
  ExecutarAnaliseResponse,
  FonteAnalise,
  AgruparPor,
  NivelAgregacao,
  ChaveAnalise,
} from "../../../models/analise.model";

interface VariavelItem {
  id: number;
  nome: string;
  metrica?: string;
}
interface CampanhaItem {
  id: number;
  nome: string;
}
interface UnidadeItem {
  id: number;
  nome: string;
  campanha_id: number;
}
interface OpcaoItem {
  id: number;
  label: string;
}

const TIPOS_COM_AMOSTRA = new Set([
  "abundancias_por_amostra",
  "abundancias_com_variaveis",
  "matriz_acumulacao",
]);

@Component({
  selector: "app-analises",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./analise.component.html",
  styleUrls: ["./analise.component.css"],
  animations: [
    trigger("fadeSlide", [
      transition(":enter", [
        style({ opacity: 0, transform: "translateY(14px)" }),
        animate(
          "260ms ease-out",
          style({ opacity: 1, transform: "translateY(0)" }),
        ),
      ]),
      transition(":leave", [
        animate(
          "160ms ease-in",
          style({ opacity: 0, transform: "translateY(6px)" }),
        ),
      ]),
    ]),
  ],
})
export class AnalisesComponent implements OnInit {
  private readonly analiseService = inject(AnaliseService);
  private readonly variavelService = inject(VariavelService);
  private readonly campanhaService = inject(CampanhaService);
  private readonly unidadeService = inject(UnidadeAmostralService);
  private readonly util = inject(UtilService);
  private readonly route = inject(ActivatedRoute);

  estudoId!: number;

  readonly catalogo = CATALOGO_ANALISES;
  readonly categorias: CategoriaAnalise[] = [
    "modelo_distribuicao",
    "rarefacao",
    "estimador_riqueza",
    "indice_diversidade",
    "teste_hipotese",
    "correlacao",
    "regressao",
    "similaridade",
    "multivariada",
    "glm",
    "acumulacao",
  ];

  variaveis: VariavelItem[] = [];
  campanhas: CampanhaItem[] = [];
  unidades: UnidadeItem[] = [];
  carregando = false;
  chaveEscolhida = "";
  analiseAtual: CatalogoAnalise | null = null;
  fonteX: FonteAnalise = "variavel";
  fonteY: FonteAnalise = "variavel";
  variavelXId: number | undefined = undefined;
  variavelYId: number | undefined = undefined;
  nivelAgregacao: NivelAgregacao = "unidade_amostral";
  fonte: FonteAnalise = "variavel";
  variavelId: number | undefined = undefined;
  agruparPor: AgruparPor = "unidade_amostral";
  grupo1Ids: number[] = [];
  grupo2Ids: number[] = [];
  nomeGrupo1 = "";
  nomeGrupo2 = "";
  variavelIdsAmbientais: number[] = [];
  filtroCampanhaIds: number[] = [];
  filtroUnidadeIds: number[] = [];
  dataInicio = "";
  dataFim = "";
  latMinMask = "";
  latMaxMask = "";
  lngMinMask = "";
  lngMaxMask = "";
  latMin: number | undefined = undefined;
  latMax: number | undefined = undefined;
  lngMin: number | undefined = undefined;
  lngMax: number | undefined = undefined;
  loading = false;
  erro: string | null = null;
  resultado: ExecutarAnaliseResponse | null = null;

  get tipoDado(): string {
    return this.analiseAtual?.tipo_dado ?? "";
  }

  get usaAmostra(): boolean {
    return TIPOS_COM_AMOSTRA.has(this.tipoDado);
  }

  get mostraFiltroCampanha(): boolean {
    return this.usaAmostra || this.tipoDado === "abundancias";
  }

  get mostraBoundingBox(): boolean {
    return !this.usaAmostra;
  }

  get mostraNivelAgregacaoDoisVetores(): boolean {
    return this.fonteX !== "variavel" || this.fonteY !== "variavel";
  }

  get mostraNivelAgregacaoVetorUnico(): boolean {
    return this.fonte !== "variavel";
  }

  get mostraNivelAgregacaoDoisGrupos(): boolean {
    return this.fonte !== "variavel";
  }

  get variavelOpcoes(): OpcaoItem[] {
    return this.variaveis.map((v) => ({
      id: v.id,
      label: v.metrica ? `${v.nome} (${v.metrica})` : v.nome,
    }));
  }

  get unidadesVisiveis(): OpcaoItem[] {
    return this.unidades.map((u) => ({ id: u.id, label: u.nome }));
  }

  get opcoesDoisGrupos(): OpcaoItem[] {
    return this.agruparPor === "campanha"
      ? this.campanhas.map((c) => ({ id: c.id, label: c.nome }))
      : this.unidadesVisiveis;
  }

  get opcoesAgruparPor(): { valor: AgruparPor; label: string }[] {
    const base = [
      { valor: "campanha" as AgruparPor, label: "Campanha" },
      { valor: "unidade_amostral" as AgruparPor, label: "Unidade amostral" },
      { valor: "evento" as AgruparPor, label: "Evento de amostragem" },
    ];
    if (this.fonte !== "variavel") {
      base.push(
        { valor: "mes" as AgruparPor, label: "Mês" },
        { valor: "ano" as AgruparPor, label: "Ano" },
        { valor: "estacao" as AgruparPor, label: "Estação do ano" },
      );
    }
    return base;
  }

  ngOnInit(): void {
    this.estudoId = Number(
      this.route.snapshot.paramMap.get("estudo_id") ??
        this.route.snapshot.queryParamMap.get("estudo_id"),
    );
    this.carregarDadosBase();
  }

  private carregarDadosBase(): void {
    this.carregando = true;
    forkJoin({
      variaveis: this.variavelService
        .listar(this.estudoId)
        .pipe(catchError(() => of([]))),
      campanhas: this.campanhaService
        .listar(this.estudoId)
        .pipe(catchError(() => of([]))),
    }).subscribe(({ variaveis, campanhas }) => {
      this.variaveis = (variaveis as VariavelItem[]).filter(
        (v: any) => v.tipo_dado === "numerico",
      );
      this.campanhas = campanhas as CampanhaItem[];

      if ((campanhas as CampanhaItem[]).length) {
        forkJoin(
          (campanhas as CampanhaItem[]).map((c) =>
            this.unidadeService
              .listar(this.estudoId, c.id)
              .pipe(catchError(() => of([]))),
          ),
        ).subscribe((grupos) => {
          this.unidades = grupos.flatMap((lista, i) =>
            (lista as any[]).map((u) => ({
              ...u,
              campanha_id: (campanhas as CampanhaItem[])[i].id,
            })),
          );
          this.carregando = false;
        });
      } else {
        this.carregando = false;
      }
    });
  }

  nomeCategoria(cat: CategoriaAnalise): string {
    return NOME_CATEGORIA[cat];
  }

  analisePorCategoria(cat: CategoriaAnalise): readonly CatalogoAnalise[] {
    return this.catalogo.filter((a) => a.categoria === cat);
  }

  onAnaliseChange(): void {
    this.analiseAtual =
      this.catalogo.find((a) => a.chave === this.chaveEscolhida) ?? null;
    this.resetFormulario();
  }

  onFonteChange(): void {
    if (this.fonte !== "variavel") this.variavelId = undefined;
    if (this.fonte === "variavel") this.agruparPor = "unidade_amostral";
    this.grupo1Ids = [];
    this.grupo2Ids = [];
  }

  onAgruparPorDoisGruposChange(): void {
    this.grupo1Ids = [];
    this.grupo2Ids = [];
  }

  private resetFormulario(): void {
    this.fonteX = "variavel";
    this.fonteY = "variavel";
    this.variavelXId = undefined;
    this.variavelYId = undefined;
    this.fonte = "variavel";
    this.variavelId = undefined;
    this.nivelAgregacao = "unidade_amostral";
    this.agruparPor = "unidade_amostral";
    this.grupo1Ids = [];
    this.grupo2Ids = [];
    this.nomeGrupo1 = "";
    this.nomeGrupo2 = "";
    this.variavelIdsAmbientais = [];
    this.filtroCampanhaIds = [];
    this.filtroUnidadeIds = [];
    this.dataInicio = "";
    this.dataFim = "";
    this.latMinMask = "";
    this.latMaxMask = "";
    this.lngMinMask = "";
    this.lngMaxMask = "";
    this.latMin = undefined;
    this.latMax = undefined;
    this.lngMin = undefined;
    this.lngMax = undefined;
    this.resultado = null;
    this.erro = null;
  }

  toggleVariavelAmbiental(id: number, ev: Event): void {
    const checked = (ev.target as HTMLInputElement).checked;
    this.variavelIdsAmbientais = checked
      ? [...this.variavelIdsAmbientais, id]
      : this.variavelIdsAmbientais.filter((v) => v !== id);
  }

  toggleGrupo(grupo: 1 | 2, id: number, ev: Event): void {
    const checked = (ev.target as HTMLInputElement).checked;
    if (grupo === 1) {
      this.grupo1Ids = checked
        ? [...this.grupo1Ids, id]
        : this.grupo1Ids.filter((v) => v !== id);
    } else {
      this.grupo2Ids = checked
        ? [...this.grupo2Ids, id]
        : this.grupo2Ids.filter((v) => v !== id);
    }
  }

  isInGrupo(grupo: 1 | 2, id: number): boolean {
    return grupo === 1
      ? this.grupo1Ids.includes(id)
      : this.grupo2Ids.includes(id);
  }

  toggleFiltro(
    lista: "filtroCampanhaIds" | "filtroUnidadeIds",
    id: number,
    ev: Event,
  ): void {
    const checked = (ev.target as HTMLInputElement).checked;
    this[lista] = checked
      ? [...this[lista], id]
      : this[lista].filter((v) => v !== id);
  }

  isFiltroAtivo(
    lista: "filtroCampanhaIds" | "filtroUnidadeIds",
    id: number,
  ): boolean {
    return this[lista].includes(id);
  }

  onCoordInput(
    campo: "latMin" | "latMax" | "lngMin" | "lngMax",
    raw: string,
  ): void {
    const masked = this.applyCoordMask(raw);
    (this as any)[campo + "Mask"] = masked;
    if (/^-?\d{1,3}°\d{2}'\d{2}"$/.test(masked)) {
      (this as any)[campo] = this.util.dmsTodecimal(masked);
    } else {
      (this as any)[campo] = undefined;
    }
  }

  private applyCoordMask(raw: string): string {
    const isNeg = raw.trimStart().startsWith("-");
    const digits = raw.replace(/\D/g, "");
    if (!digits) return isNeg ? "-" : "";
    const d1 = digits.slice(0, 3);
    const d2 = digits.slice(3, 5);
    const d3 = digits.slice(5, 7);
    let r = (isNeg ? "-" : "") + d1;
    if (digits.length > 3) r += `°${d2}`;
    if (digits.length > 5) r += `'${d3}"`;
    else if (digits.length === 5) r += `'`;
    return r;
  }

  executar(): void {
    if (!this.analiseAtual) return;

    const payload: ExecutarAnaliseRequest = {
      chave: this.chaveEscolhida as ChaveAnalise,
    };

    switch (this.tipoDado) {
      case "abundancias_com_variaveis":
        payload.variavel_ids = this.variavelIdsAmbientais;
        break;

      case "dois_vetores":
        if (this.fonteX === "variavel") {
          payload.variavel_x_id = this.variavelXId;
        } else {
          payload.fonte_x = this.fonteX;
        }
        if (this.fonteY === "variavel") {
          payload.variavel_y_id = this.variavelYId;
        } else {
          payload.fonte_y = this.fonteY;
        }
        if (this.fonteX !== "variavel" || this.fonteY !== "variavel") {
          payload.nivel_agregacao = this.nivelAgregacao;
        }
        break;

      case "vetor_unico":
        payload.fonte = this.fonte;
        if (this.fonte === "variavel") {
          payload.variavel_id = this.variavelId;
        } else {
          payload.nivel_agregacao = this.nivelAgregacao;
        }
        break;

      case "dois_grupos":
        payload.fonte = this.fonte;
        if (this.fonte === "variavel") {
          payload.variavel_id = this.variavelId;
        } else {
          payload.nivel_agregacao = this.nivelAgregacao;
        }
        payload.grupo1_ids = this.grupo1Ids;
        payload.grupo2_ids = this.grupo2Ids;
        if (this.nomeGrupo1) payload.nome_grupo1 = this.nomeGrupo1;
        if (this.nomeGrupo2) payload.nome_grupo2 = this.nomeGrupo2;
        break;

      case "multiplos_grupos":
        payload.fonte = this.fonte;
        if (this.fonte === "variavel") payload.variavel_id = this.variavelId;
        payload.agrupar_por = this.agruparPor;
        break;
    }

    if (this.filtroCampanhaIds.length)
      payload.campanha_ids = this.filtroCampanhaIds;
    if (this.filtroUnidadeIds.length)
      payload.unidade_ids = this.filtroUnidadeIds;
    if (this.dataInicio) payload.data_inicio = this.dataInicio;
    if (this.dataFim) payload.data_fim = this.dataFim;
    if (this.latMin !== undefined) payload.latitude_min = this.latMin;
    if (this.latMax !== undefined) payload.latitude_max = this.latMax;
    if (this.lngMin !== undefined) payload.longitude_min = this.lngMin;
    if (this.lngMax !== undefined) payload.longitude_max = this.lngMax;

    this.loading = true;
    this.erro = null;
    this.resultado = null;

    this.analiseService.executar(this.estudoId, payload).subscribe({
      next: (res) => {
        this.resultado = res;
        this.loading = false;
      },
      error: (err) => {
        this.erro =
          err?.error?.erro ??
          err?.error?.message ??
          "Erro ao executar a análise. Verifique os parâmetros.";
        this.loading = false;
      },
    });
  }

  exportar(): void {
    if (!this.resultado?.urlArquivo) return;
    const a = document.createElement("a");
    a.href = this.resultado.urlArquivo;
    a.download = "";
    a.click();
  }

  abrirChatbot(): void {
    /* TODO */
  }
}
