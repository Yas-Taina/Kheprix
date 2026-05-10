import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { UnidadeAmostralService } from "../../../core/services/unidade-amostral.service";
import { VariavelService } from "../../../core/services/variavel.service";
import { Variavel, ValorVariavel } from "../../../models";
import { UtilService } from "../../../core/services/util.service";
import { DmsMaskDirective } from "../../../core/directives/dms-mask.directive";

@Component({
  selector: "app-unidade-novo",
  standalone: true,
  templateUrl: "./unidade-novo.component.html",
  styleUrls: ["./unidade-novo.component.css"],
  imports: [CommonModule, FormsModule, DmsMaskDirective],
})
export class UnidadeNovoComponent implements OnInit {
  estudoId!: number;
  campanhaId!: number;
  unidadeId: number | null = null;
  isEdit = false;
  nome = "";
  latDMS = "";
  lngDMS = "";
  raio: number | null = null;
  metodoColeta = "";
  esforcoAmostral = "";
  variaveis: Variavel[] = [];
  valoresVars: ValorVariavel[] = [];
  loading = false;
  gpsLoading = false;
  erro = "";
  gpsErro = "";

  constructor(
    private unidadeService: UnidadeAmostralService,
    private variavelService: VariavelService,
    public router: Router,
    private route: ActivatedRoute,
    public util: UtilService,
  ) {}
 
  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.campanhaId = +this.route.snapshot.params["campanha_id"];
    this.unidadeId = this.route.snapshot.params["unidade_id"]
      ? +this.route.snapshot.params["unidade_id"]
      : null;
    this.isEdit = !!this.unidadeId;

    this.variavelService.listar(this.estudoId, "unidade").subscribe((vars) => {
      this.variaveis = vars;
      this.valoresVars = vars.map((v) => ({ variavel_id: v.id, valor: "" }));
    });

    if (this.isEdit && this.unidadeId) {
      this.unidadeService
        .buscar(this.estudoId, this.campanhaId, this.unidadeId)
        .subscribe((u) => {
          this.nome = u.nome;
          this.latDMS = this.util.decimalToDMS(u.latitude, "lat");
          this.lngDMS = this.util.decimalToDMS(u.longitude, "lng");
          this.raio = u.raio;
          this.metodoColeta = u.metodo_coleta ?? "";
          this.esforcoAmostral = u.esforco_amostral ?? "";
        });
    }
  }

  async obterLocalizacao() {
    this.gpsLoading = true;
    this.gpsErro = "";
    try {
      const loc = await this.util.getCurrentLocationDMS();
      this.latDMS = loc.latDMS;
      this.lngDMS = loc.lngDMS;
    } catch {
      this.gpsErro =
        "Não foi possível obter a localização. Você pode preencher manualmente.";
    } finally {
      this.gpsLoading = false;
    }
  }

  salvar() {
    if (!this.nome || !this.latDMS || !this.lngDMS) {
      this.erro = "Nome e coordenadas são obrigatórios.";
      return;
    }
    this.loading = true;
    this.erro = "";
    const lat = this.util.dmsTodecimal(this.latDMS);
    const lng = this.util.dmsTodecimal(this.lngDMS);
    const payload = {
      nome: this.nome,
      latitude: lat,
      longitude: lng,
      raio: this.raio ?? undefined,
      metodo_coleta: this.metodoColeta || undefined,
      esforco_amostral: this.esforcoAmostral || undefined,
      valores_variaveis: this.valoresVars.filter(
        (v) => v.valor !== "" && v.valor !== null && v.valor !== undefined,
      ),
    };
    const obs =
      this.isEdit && this.unidadeId
        ? this.unidadeService.atualizar(
            this.estudoId,
            this.campanhaId,
            this.unidadeId,
            payload,
          )
        : this.unidadeService.criar(this.estudoId, this.campanhaId, payload);
    obs.subscribe({
      next: () => this.voltar(),
      error: () => {
        this.erro = "Erro ao salvar.";
        this.loading = false;
      },
    });
  }

  voltar() {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "campanhas",
      this.campanhaId,
      "unidades",
    ]);
  }
}