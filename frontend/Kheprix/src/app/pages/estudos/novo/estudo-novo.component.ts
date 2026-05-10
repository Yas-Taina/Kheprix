import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { EstudoService } from "../../../core/services/estudo.service";
import { VariavelService } from "../../../core/services/variavel.service";
import { VariavelCreate, Variavel } from "../../../models";

interface VariavelForm extends VariavelCreate {
  _id: number;
}

@Component({
  selector: "app-estudo-novo",
  standalone: true,
  templateUrl: "./estudo-novo.component.html",
  styleUrls: ["./estudo-novo.component.css"],
  imports: [CommonModule, FormsModule],
})
export class EstudoNovoComponent implements OnInit {
  nome = "";
  observacoes = "";
  variaveis: VariavelForm[] = [];
  variaveisEdit: Variavel[] = [];
  isEdit = false;
  estudoId: number | null = null;
  loading = false;
  erro = "";
  private _counter = 0;

  constructor(
    private estudoService: EstudoService,
    private variavelService: VariavelService,
    public router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.estudoId = this.route.snapshot.params["estudo_id"]
      ? +this.route.snapshot.params["estudo_id"]
      : null;
    this.isEdit = !!this.estudoId;

    if (this.isEdit && this.estudoId) {
      this.estudoService.listar().subscribe((lista) => {
        const estudo = lista.find((e) => e.id === this.estudoId);
        if (estudo) {
          this.nome = estudo.nome;
          this.observacoes = estudo.observacoes ?? "";
        }
      });
      this.variavelService.listar(this.estudoId).subscribe((vars) => {
        this.variaveisEdit = vars;
      });
    }
  }

  addVariavel(): void {
    this.variaveis.push({
      _id: ++this._counter,
      nome: "",
      nivel_aplicacao: "campanha",
      tipo_dado: "numerico",
      metrica: "",
    });
  }

  removeVariavel(i: number): void {
    this.variaveis.splice(i, 1);
  }

  salvar(): void {
    if (!this.nome.trim()) {
      this.erro = "Informe o nome do estudo.";
      return;
    }
    this.loading = true;
    this.erro = "";

    if (this.isEdit && this.estudoId) {
      this.estudoService
        .atualizar(this.estudoId, {
          nome: this.nome,
          observacoes: this.observacoes,
        })
        .subscribe({
          next: () => this.router.navigate(["/estudos", this.estudoId]),
          error: () => {
            this.erro = "Erro ao salvar.";
            this.loading = false;
          },
        });
    } else {
      const payload = {
        nome: this.nome,
        observacoes: this.observacoes,
        variaveis: this.variaveis.map(({ _id, ...v }) => v),
      };
      this.estudoService.criar(payload).subscribe({
        next: (e) => this.router.navigate(["/estudos", e.id]),
        error: () => {
          this.erro = "Erro ao criar estudo.";
          this.loading = false;
        },
      });
    }
  }
}
