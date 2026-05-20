import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { EventoAmostragemService } from "../../../core/services/evento-amostragem.service";
import { VariavelService } from "../../../core/services/variavel.service";
import { Variavel, ValorVariavel, EventoAmostragem } from "../../../models";
import { extrairMensagemErro } from "../../../core/utils/erro.util";

@Component({
  selector: "app-evento-novo",
  standalone: true,
  templateUrl: "./evento-novo.component.html",
  styleUrls: ["./evento-novo.component.css"],
  imports: [CommonModule, FormsModule],
})
export class EventoNovoComponent implements OnInit {
  estudoId!: number;
  campanhaId!: number;
  unidadeId!: number;
  eventoId: number | null = null;
  isEdit = false;
  dataInicio = "";
  horaInicio = "";
  esforcoReal = "";
  variaveis: Variavel[] = [];
  valoresVars: ValorVariavel[] = [];
  loading = false;
  erro = "";

  private eventoCarregado: EventoAmostragem | null = null;

  constructor(
    private eventoService: EventoAmostragemService,
    private variavelService: VariavelService,
    public router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.campanhaId = +this.route.snapshot.params["campanha_id"];
    this.unidadeId = +this.route.snapshot.params["unidade_id"];
    this.eventoId = this.route.snapshot.params["evento_id"]
      ? +this.route.snapshot.params["evento_id"]
      : null;
    this.isEdit = !!this.eventoId;

    this.variavelService.listar(this.estudoId, "evento").subscribe((vars) => {
      this.variaveis = vars;
      this.valoresVars = vars.map((v) => {
        const existente = this.eventoCarregado?.valores_variaveis?.find(
          (val) => val.variavel_id === v.id,
        );
        return { variavel_id: v.id, valor: existente?.valor ?? "" };
      });
    });

    if (this.isEdit && this.eventoId) {
      this.eventoService
        .buscar(this.estudoId, this.campanhaId, this.unidadeId, this.eventoId)
        .subscribe((ev) => {
          const dtInicio = new Date(ev.horario_inicio);
          this.eventoCarregado = ev;
          this.dataInicio = dtInicio.toISOString().split("T")[0];
          this.horaInicio = dtInicio.toTimeString().substring(0, 5);
          this.esforcoReal = ev.esforco_real ?? "";
          if (this.variaveis.length > 0) {
            this.valoresVars = this.variaveis.map((v) => {
              const existente = ev.valores_variaveis?.find(
                (val) => val.variavel_id === v.id,
              );
              return { variavel_id: v.id, valor: existente?.valor ?? "" };
            });
          }
        });
    }
  }

  salvar() {
    if (!this.dataInicio || !this.horaInicio) {
      this.erro = "Data e hora de início são obrigatórias.";
      return;
    }
    this.loading = true;
    this.erro = "";
    const horarioInicio = `${this.dataInicio}T${this.horaInicio}:00`;
    const payload = {
      horario_inicio: horarioInicio,
      esforco_real: this.esforcoReal || undefined,
      valores_variaveis: this.valoresVars.filter(
        (v) => v.valor !== "" && v.valor !== null && v.valor !== undefined,
      ),
    };
    const obs =
      this.isEdit && this.eventoId
        ? this.eventoService.atualizar(
            this.estudoId,
            this.campanhaId,
            this.unidadeId,
            this.eventoId,
            payload,
          )
        : this.eventoService.criar(
            this.estudoId,
            this.campanhaId,
            this.unidadeId,
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
    ]);
  }
}
