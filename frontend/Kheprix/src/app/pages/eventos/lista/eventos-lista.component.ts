import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, ActivatedRoute } from "@angular/router";
import { EventoAmostragemService } from "../../../core/services/evento-amostragem.service";
import { UnidadeAmostralService } from "../../../core/services/unidade-amostral.service";
import { EstudoService } from "../../../core/services/estudo.service";
import { EventoAmostragem, UnidadeAmostral } from "../../../models";
import { UtilService } from "../../../core/services/util.service";

@Component({
  selector: "app-eventos-lista",
  standalone: true,
  templateUrl: "./eventos-lista.component.html",
  styleUrls: ["./eventos-lista.component.css"],
  imports: [CommonModule],
})
export class EventosListaComponent implements OnInit {
  eventos: EventoAmostragem[] = [];
  unidadeDetalhe: UnidadeAmostral | null = null;
  estudoId!: number;
  perfilEstudo = "";
  campanhaId!: number;
  unidadeId!: number;
  nomeUnidade = "";
  loading = true;
  showDetalhes = false;

  constructor(
    private eventoService: EventoAmostragemService,
    private unidadeService: UnidadeAmostralService,
    private estudoService: EstudoService,
    public router: Router,
    private route: ActivatedRoute,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.campanhaId = +this.route.snapshot.params["campanha_id"];
    this.unidadeId = +this.route.snapshot.params["unidade_id"];
    this.eventoService
      .listar(this.estudoId, this.campanhaId, this.unidadeId)
      .subscribe({
        next: (ev) => {
          this.eventos = ev;
          this.loading = false;
        },
        error: () => (this.loading = false),
      });
    this.unidadeService
      .buscar(this.estudoId, this.campanhaId, this.unidadeId)
      .subscribe((u) => {
        this.unidadeDetalhe = u;
        this.nomeUnidade = u.nome;
      });

    this.estudoService.listar().subscribe((l) => {
      const estudo = l.find((e) => e.id === this.estudoId);
      this.perfilEstudo = estudo?.perfil ?? "";
    });
  }

  toggleDetalhes() {
    this.showDetalhes = !this.showDetalhes;
  }

  isProprietario(): boolean {
    return this.perfilEstudo === "proprietario";
  }

  verRegistros(ev: EventoAmostragem) {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "campanhas",
      this.campanhaId,
      "unidades",
      this.unidadeId,
      "eventos",
      ev.id,
      "registros",
    ]);
  }

  novoEvento() {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "campanhas",
      this.campanhaId,
      "unidades",
      this.unidadeId,
      "eventos",
      "novo",
    ]);
  }

  editarUnidade() {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "campanhas",
      this.campanhaId,
      "unidades",
      this.unidadeId,
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
    ]);
  }

  deletar(ev: EventoAmostragem) {
    if (!confirm("Excluir este evento?")) return;
    this.eventoService
      .deletar(this.estudoId, this.campanhaId, this.unidadeId, ev.id)
      .subscribe({
        next: () => (this.eventos = this.eventos.filter((x) => x.id !== ev.id)),
      });
  }
}
