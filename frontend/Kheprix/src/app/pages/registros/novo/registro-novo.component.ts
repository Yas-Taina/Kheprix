import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { RegistroOcorrenciaService } from "../../../core/services/registro-ocorrencia.service";
import { EspecieService } from "../../../core/services/especie.service";
import { VariavelService } from "../../../core/services/variavel.service";
import {
  Especie,
  Variavel,
  ValorVariavel,
  RegistroOcorrencia,
} from "../../../models";
import { UtilService } from "../../../core/services/util.service";
import { environment } from "../../../../environments/environment";
import { extrairMensagemErro } from "../../../core/utils/erro.util";
import { DmsMaskDirective } from "../../../core/directives/dms-mask.directive";

@Component({
  selector: "app-registro-novo",
  standalone: true,
  templateUrl: "./registro-novo.component.html",
  styleUrls: ["./registro-novo.component.css"],
  imports: [CommonModule, FormsModule, DmsMaskDirective],
})
export class RegistroNovoComponent implements OnInit {
  estudoId!: number;
  campanhaId!: number;
  unidadeId!: number;
  eventoId!: number;
  registroId: number | null = null;
  isEdit = false;
  data = "";
  hora = "";
  latDMS = "";
  lngDMS = "";
  especieId: number | null = null;
  qtdeIndividuos: number | null = null;
  ausenciaEspecie = false;
  fotoBase64 = "";
  fotoNome = "";
  fotoPreview = "";
  especies: Especie[] = [];
  variaveis: Variavel[] = [];
  valoresVars: ValorVariavel[] = [];
  loading = false;
  gpsLoading = false;
  erro = "";
  gpsErro = "";
  apiUrl = environment.apiUrl;

  private registroCarregado: RegistroOcorrencia | null = null;

  constructor(
    private registroService: RegistroOcorrenciaService,
    private especieService: EspecieService,
    private variavelService: VariavelService,
    public router: Router,
    private route: ActivatedRoute,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.campanhaId = +this.route.snapshot.params["campanha_id"];
    this.unidadeId = +this.route.snapshot.params["unidade_id"];
    this.eventoId = +this.route.snapshot.params["evento_id"];
    this.registroId = this.route.snapshot.params["registro_id"]
      ? +this.route.snapshot.params["registro_id"]
      : null;
    this.isEdit = !!this.registroId;

    this.especieService
      .listar(this.estudoId)
      .subscribe((e) => (this.especies = e));

    this.variavelService.listar(this.estudoId, "registro").subscribe((vars) => {
      this.variaveis = vars;
      this.valoresVars = vars.map((v) => {
        const existente = this.registroCarregado?.valores_variaveis?.find(
          (val) => val.variavel_id === v.id,
        );
        return { variavel_id: v.id, valor: existente?.valor ?? "" };
      });
    });

    if (this.isEdit && this.registroId) {
      this.registroService
        .buscar(
          this.estudoId,
          this.campanhaId,
          this.unidadeId,
          this.eventoId,
          this.registroId,
        )
        .subscribe((r) => {
          this.registroCarregado = r;
          this.data = r.data;
          this.hora = r.hora;
          this.latDMS = this.util.decimalToDMS(r.latitude, "lat");
          this.lngDMS = this.util.decimalToDMS(r.longitude, "lng");
          this.especieId = r.especie_id;
          this.qtdeIndividuos = r.qtde_individuos;
          this.ausenciaEspecie = r.ausencia_especie;
          if (r.foto)
            this.fotoPreview = this.util.buildFotoUrl(this.apiUrl, r.foto);
          if (this.variaveis.length > 0) {
            this.valoresVars = this.variaveis.map((v) => {
              const existente = r.valores_variaveis?.find(
                (val) => val.variavel_id === v.id,
              );
              return { variavel_id: v.id, valor: existente?.valor ?? "" };
            });
          }
        });
    }
  }

  abrirArquivo() {
    this.util.openFilePicker((b64, name) => {
      this.fotoBase64 = b64;
      this.fotoNome = name;
      this.fotoPreview = b64;
    });
  }
  abrirCamera() {
    this.util.openCamera((b64) => {
      this.fotoBase64 = b64;
      this.fotoNome = "foto.jpg";
      this.fotoPreview = b64;
    });
  }
  ajustarQtde() {
    if (this.qtdeIndividuos != null) {
      this.qtdeIndividuos = Math.max(0, Math.floor(this.qtdeIndividuos));
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
        "Não foi possível obter a localização. Tente preencher manualmente.";
    } finally {
      this.gpsLoading = false;
    }
  }

  salvar() {
    if (
      !this.data ||
      !this.hora ||
      !this.latDMS ||
      !this.lngDMS ||
      !this.especieId
    ) {
      this.erro = "Preencha data, hora, coordenadas e espécie.";
      return;
    }
    const lat = this.util.dmsTodecimal(this.latDMS);
    const lng = this.util.dmsTodecimal(this.lngDMS);
    if (lat < -90 || lat > 90) {
      this.erro = "Latitude deve estar entre -90° e 90°.";
      return;
    }
    if (lng < -180 || lng > 180) {
      this.erro = "Longitude deve estar entre -180° e 180°.";
      return;
    }
    if (
      this.qtdeIndividuos != null &&
      (this.qtdeIndividuos < 0 || this.qtdeIndividuos > 1_000_000)
    ) {
      this.erro = "Quantidade de indivíduos deve estar entre 0 e 1.000.000.";
      return;
    }
    this.loading = true;
    this.erro = "";
    const payload = {
      especie_id: this.especieId!,
      data: this.data,
      hora: this.hora,
      latitude: lat,
      longitude: lng,
      qtde_individuos: this.qtdeIndividuos ?? undefined,
      ausencia_especie: this.ausenciaEspecie,
      ...(this.fotoBase64 ? { foto: this.fotoBase64 } : {}),
      valores_variaveis: this.valoresVars.filter(
        (v) => v.valor !== "" && v.valor !== null && v.valor !== undefined,
      ),
    };
    const obs =
      this.isEdit && this.registroId
        ? this.registroService.atualizar(
            this.estudoId,
            this.campanhaId,
            this.unidadeId,
            this.eventoId,
            this.registroId,
            payload,
          )
        : this.registroService.criar(
            this.estudoId,
            this.campanhaId,
            this.unidadeId,
            this.eventoId,
            payload,
          );
    obs.subscribe({
      next: () => this.voltar(),
      error: (err) => {
        this.erro = extrairMensagemErro(err, "Erro ao salvar.");
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
      this.unidadeId,
      "eventos",
      this.eventoId,
      "registros",
    ]);
  }
}
