import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, ActivatedRoute } from "@angular/router";
import { CampanhaService } from "../../../core/services/campanha.service";
import { EstudoService } from "../../../core/services/estudo.service";
import { Campanha } from "../../../models";
import { UtilService } from "../../../core/services/util.service";

@Component({
  selector: "app-campanhas-lista",
  standalone: true,
  templateUrl: "./campanhas-lista.component.html",
  styleUrls: ["./campanhas-lista.component.css"],
  imports: [CommonModule],
})
export class CampanhasListaComponent implements OnInit {
  campanhas: Campanha[] = [];
  estudoId!: number;
  nomeEstudo = "";
  loading = true;

  constructor(
    private campanhaService: CampanhaService,
    private estudoService: EstudoService,
    public router: Router,
    private route: ActivatedRoute,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.campanhaService.listar(this.estudoId).subscribe({
      next: (c) => {
        this.campanhas = c;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
    this.estudoService
      .listar()
      .subscribe(
        (l) =>
          (this.nomeEstudo = l.find((e) => e.id === this.estudoId)?.nome ?? ""),
      );
  }

  verUnidades(c: Campanha) {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "campanhas",
      c.id,
      "unidades",
    ]);
  }
  deletar(c: Campanha) {
    if (!confirm(`Excluir "${c.nome}"?`)) return;
    this.campanhaService
      .deletar(this.estudoId, c.id)
      .subscribe({
        next: () =>
          (this.campanhas = this.campanhas.filter((x) => x.id !== c.id)),
      });
  }
}
