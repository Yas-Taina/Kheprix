import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, ActivatedRoute } from "@angular/router";
import { UnidadeAmostralService } from "../../../core/services/unidade-amostral.service";
import { CampanhaService } from "../../../core/services/campanha.service";
import { EstudoService} from "../../../core/services/estudo.service";
import { UnidadeAmostral, Campanha } from "../../../models";
import { UtilService } from "../../../core/services/util.service";

@Component({
  selector: "app-unidades-lista",
  standalone: true,
  templateUrl: "./unidades-lista.component.html",
  styleUrls: ["./unidades-lista.component.css"],
  imports: [CommonModule],
})
export class UnidadesListaComponent implements OnInit {
  unidades: UnidadeAmostral[] = [];
  campanhaDetalhe: Campanha | null = null;
  estudoId!: number;
  campanhaId!: number;
  nomeCampanha = "";
  perfilEstudo = "";
  loading = true;
  showDetalhes = false;

  constructor(
    private unidadeService: UnidadeAmostralService,
    private campanhaService: CampanhaService,
    private estudoService: EstudoService,
    public router: Router,
    private route: ActivatedRoute,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.campanhaId = +this.route.snapshot.params["campanha_id"];
    this.unidadeService.listar(this.estudoId, this.campanhaId).subscribe({
      next: (u) => {
        this.unidades = u;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
    this.campanhaService
      .buscar(this.estudoId, this.campanhaId)
      .subscribe((c) => {
        this.campanhaDetalhe = c;
        this.nomeCampanha = c.nome;
      });
    
    this.estudoService.listar().subscribe((l) => {
      const estudo = l.find((e) => e.id === this.estudoId);
      this.perfilEstudo = estudo?.perfil ?? "";
    });
  }

  isProprietario(): boolean {
    return this.perfilEstudo === "proprietario";
  }

  toggleDetalhes() {
    this.showDetalhes = !this.showDetalhes;
  }

  formatCoord(dec: number): string {
    return this.util.decimalToDMS(dec);
  }

  verEventos(u: UnidadeAmostral) {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "campanhas",
      this.campanhaId,
      "unidades",
      u.id,
      "eventos",
    ]);
  }

  editarCampanha() {
    this.router.navigate([
      "/estudos",
      this.estudoId,
      "campanhas",
      this.campanhaId,
      "editar",
    ]);
  }

  deletar(u: UnidadeAmostral) {
    if (!confirm(`Excluir "${u.nome}"?`)) return;
    this.unidadeService
      .deletar(this.estudoId, this.campanhaId, u.id)
      .subscribe({
        next: () =>
          (this.unidades = this.unidades.filter((x) => x.id !== u.id)),
      });
  }
}
