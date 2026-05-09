import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { EspecieService } from "../../../core/services/especie.service";
import { EstudoService } from "../../../core/services/estudo.service";
import { Especie, StatusConservacaoLabels } from "../../../models";
import { UtilService } from "../../../core/services/util.service";
import { environment } from "../../../../environments/environment";

@Component({
  selector: "app-especies-lista",
  standalone: true,
  templateUrl: "./especies-lista.component.html",
  styleUrls: ["./especies-lista.component.css"],
  imports: [CommonModule, FormsModule],
})
export class EspeciesListaComponent implements OnInit {
  especies: Especie[] = [];
  estudoId!: number;
  nomeEstudo = "";
  loading = true;
  busca = "";
  apiUrl = environment.apiUrl;
  placeholderImg =
    'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="52" height="52" viewBox="0 0 52 52"%3E%3Ccircle cx="26" cy="26" r="25" fill="%23D4CDBA"/%3E%3Ctext x="50%25" y="55%25" text-anchor="middle" font-size="10" fill="%238A7D6E"%3EFoto%3C/text%3E%3C/svg%3E';

  constructor(
    private especieService: EspecieService,
    private estudoService: EstudoService,
    public router: Router,
    private route: ActivatedRoute,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.carregar();
    this.estudoService.listar().subscribe((lista) => {
      this.nomeEstudo = lista.find((e) => e.id === this.estudoId)?.nome ?? "";
    });
  }

  carregar() {
    this.loading = true;
    this.especieService
      .listar(this.estudoId, this.busca || undefined)
      .subscribe({
        next: (e) => {
          this.especies = e;
          this.loading = false;
        },
        error: () => (this.loading = false),
      });
  }

  filtrar() {
    this.carregar();
  }
  verDetalhe(e: Especie) {
    this.router.navigate(["/estudos", this.estudoId, "especies", e.id]);
  }
}
