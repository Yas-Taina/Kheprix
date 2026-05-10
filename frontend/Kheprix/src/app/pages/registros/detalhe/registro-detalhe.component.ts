import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, ActivatedRoute } from "@angular/router";
import { RegistroOcorrenciaService } from "../../../core/services/registro-ocorrencia.service";
import { EspecieService } from "../../../core/services/especie.service";
import { VariavelService } from "../../../core/services/variavel.service";
import { RegistroOcorrencia, Especie, Variavel } from "../../../models";
import { UtilService } from "../../../core/services/util.service";
import { environment } from "../../../../environments/environment";

@Component({
  selector: "app-registro-detalhe",
  standalone: true,
  templateUrl: "./registro-detalhe.component.html",
  styleUrls: ["./registro-detalhe.component.css"],
  imports: [CommonModule],
})
export class RegistroDetalheComponent implements OnInit {
  registro: RegistroOcorrencia | null = null;
  especie: Especie | null = null;
  variaveis: Variavel[] = [];
  valoresExibicao: string[] = [];
  loading = true;
  estudoId!: number;
  campanhaId!: number;
  unidadeId!: number;
  eventoId!: number;
  registroId!: number;
  apiUrl = environment.apiUrl;
  placeholderImg =
    'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="180" height="180"%3E%3Ccircle cx="90" cy="90" r="88" fill="%23D4CDBA"/%3E%3Ctext x="50%25" y="55%25" text-anchor="middle" font-size="14" fill="%238A7D6E"%3EFoto%3C/text%3E%3C/svg%3E';

  constructor(
    private registroService: RegistroOcorrenciaService,
    private especieService: EspecieService,
    private variavelService: VariavelService,
    private route: ActivatedRoute,
    public router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.campanhaId = +this.route.snapshot.params["campanha_id"];
    this.unidadeId = +this.route.snapshot.params["unidade_id"];
    this.eventoId = +this.route.snapshot.params["evento_id"];
    this.registroId = +this.route.snapshot.params["registro_id"];

    this.registroService
      .buscar(
        this.estudoId,
        this.campanhaId,
        this.unidadeId,
        this.eventoId,
        this.registroId,
      )
      .subscribe((r) => {
        this.registro = r;
        this.especieService
          .buscar(this.estudoId, r.especie_id)
          .subscribe((e) => (this.especie = e));
        this.loading = false;
      });

    this.variavelService.listar(this.estudoId, "registro").subscribe((vars) => {
      this.variaveis = vars;
      this.valoresExibicao = vars.map(() => "");
    });
  }

  editar() {
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
      this.registroId,
      "editar",
    ]);
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

  deletar() {
    if (!confirm("Excluir este registro?")) return;
    this.registroService
      .deletar(
        this.estudoId,
        this.campanhaId,
        this.unidadeId,
        this.eventoId,
        this.registroId,
      )
      .subscribe({
        next: () => this.voltar(),
      });
  }
}
