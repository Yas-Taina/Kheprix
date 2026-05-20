import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { EstudoService } from "../../../core/services/estudo.service";
import { VariavelCreate } from "../../../models";
import { extrairMensagemErro } from "../../../core/utils/erro.util";

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
export class EstudoNovoComponent {
  nome = "";
  observacoes = "";
  variaveis: VariavelForm[] = [];
  loading = false;
  erro = "";
  private _counter = 0;

  constructor(
    private estudoService: EstudoService,
    public router: Router,
    private route: ActivatedRoute,
  ) {}

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

    const payload = {
      nome: this.nome,
      observacoes: this.observacoes,
      variaveis: this.variaveis.map(({ _id, ...v }) => v),
    };
    this.estudoService.criar(payload).subscribe({
      next: (e) => this.router.navigate(["/estudos", e.id]),
      error: (err) => {
        this.erro = extrairMensagemErro(err, "Erro ao criar estudo.");
        this.loading = false;
      },
    });
  }
}
