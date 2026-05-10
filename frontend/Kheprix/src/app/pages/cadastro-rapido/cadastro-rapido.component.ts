import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { EstudoService } from "../../core/services/estudo.service";
import { CampanhaService } from "../../core/services/campanha.service";
import { UnidadeAmostralService } from "../../core/services/unidade-amostral.service";
import { EventoAmostragemService } from "../../core/services/evento-amostragem.service";
import {
  Estudo,
  Campanha,
  UnidadeAmostral,
  EventoAmostragem,
} from "../../models";
import { UtilService } from "../../core/services/util.service";

@Component({
  selector: "app-cadastro-rapido",
  standalone: true,
  templateUrl: "./cadastro-rapido.component.html",
  styleUrls: ["./cadastro-rapido.component.css"],
  imports: [CommonModule, FormsModule],
})
export class CadastroRapidoComponent implements OnInit {
  estudos: Estudo[] = [];
  campanhas: Campanha[] = [];
  unidades: UnidadeAmostral[] = [];
  eventos: EventoAmostragem[] = [];

  estudoId: number | "" = "";
  campanhaId: number | "" = "";
  unidadeId: number | "" = "";
  eventoId: number | "" = "";

  constructor(
    private estudoService: EstudoService,
    private campanhaService: CampanhaService,
    private unidadeService: UnidadeAmostralService,
    private eventoService: EventoAmostragemService,
    private router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoService.listar().subscribe((e) => (this.estudos = e));
  }

  onEstudoChange() {
    this.campanhaId = "";
    this.unidadeId = "";
    this.eventoId = "";
    this.campanhas = [];
    this.unidades = [];
    this.eventos = [];
    if (this.estudoId) {
      this.campanhaService
        .listar(+this.estudoId)
        .subscribe((c) => (this.campanhas = c));
    }
  }

  onCampanhaChange() {
    this.unidadeId = "";
    this.eventoId = "";
    this.unidades = [];
    this.eventos = [];
    if (this.estudoId && this.campanhaId) {
      this.unidadeService
        .listar(+this.estudoId, +this.campanhaId)
        .subscribe((u) => (this.unidades = u));
    }
  }

  onUnidadeChange() {
    this.eventoId = "";
    this.eventos = [];
    if (this.estudoId && this.campanhaId && this.unidadeId) {
      this.eventoService
        .listar(+this.estudoId, +this.campanhaId, +this.unidadeId)
        .subscribe((ev) => (this.eventos = ev));
    }
  }

  prosseguir() {
    if (!this.estudoId || !this.campanhaId || !this.unidadeId || !this.eventoId)
      return;
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
      "novo",
    ]);
  }
}
