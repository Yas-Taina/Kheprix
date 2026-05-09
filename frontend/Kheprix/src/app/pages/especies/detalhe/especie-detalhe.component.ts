import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, ActivatedRoute } from "@angular/router";
import { EspecieService } from "../../../core/services/especie.service";
import { Especie, StatusConservacaoLabels } from "../../../models";
import { UtilService } from "../../../core/services/util.service";
import { environment } from "../../../../environments/environment";

@Component({
  selector: "app-especie-detalhe",
  standalone: true,
  templateUrl: "./especie-detalhe.component.html",
  styleUrls: ["./especie-detalhe.component.css"],
  imports: [CommonModule],
})
export class EspecieDetalheComponent implements OnInit {
  especie: Especie | null = null;
  loading = true;
  estudoId!: number;
  especieId!: number;
  apiUrl = environment.apiUrl;
  placeholderImg =
    'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="180" height="180" viewBox="0 0 180 180"%3E%3Ccircle cx="90" cy="90" r="88" fill="%23D4CDBA"/%3E%3Ctext x="50%25" y="55%25" text-anchor="middle" font-size="14" fill="%238A7D6E"%3EFoto%3C/text%3E%3C/svg%3E';

  constructor(
    private especieService: EspecieService,
    private route: ActivatedRoute,
    public router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.especieId = +this.route.snapshot.params["especie_id"];
    this.especieService.buscar(this.estudoId, this.especieId).subscribe({
      next: (e) => {
        this.especie = e;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  statusLabel(s: string): string {
    return (StatusConservacaoLabels as any)[s] || s;
  }
  editar() {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "especies",
      this.especieId,
      "editar",
    ]);
  }
  voltar() {
    this.router.navigate(["/estudos", this.estudoId, "especies"]);
  }
  deletar() {
    if (!confirm("Excluir esta espécie?")) return;
    this.especieService.deletar(this.estudoId, this.especieId).subscribe({
      next: () => this.voltar(),
    });
  }
}
