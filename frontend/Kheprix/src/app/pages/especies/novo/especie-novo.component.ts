import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router";
import { EspecieService } from "../../../core/services/especie.service";
import { StatusConservacao, StatusConservacaoLabels } from "../../../models";
import { UtilService } from "../../../core/services/util.service";
import { environment } from "../../../../environments/environment";
import { extrairMensagemErro } from "../../../core/utils/erro.util";

@Component({
  selector: "app-especie-novo",
  standalone: true,
  templateUrl: "./especie-novo.component.html",
  styleUrls: ["./especie-novo.component.css"],
  imports: [CommonModule, FormsModule],
})
export class EspecieNovoComponent implements OnInit {
  estudoId!: number;
  especieId: number | null = null;
  isEdit = false;
  classe = "Insecta";
  ordem = "";
  familia = "";
  genero = "";
  especie = "";
  nomePopular = "";
  statusConservacao: StatusConservacao = "LC";
  endemismo = false;
  fotoBase64 = "";
  fotoNome = "";
  fotoPreview = "";
  loading = false;
  erro = "";
  apiUrl = environment.apiUrl;

  statusList = Object.entries(StatusConservacaoLabels).map(
    ([value, label]) => ({ value, label }),
  );

  constructor(
    private especieService: EspecieService,
    private route: ActivatedRoute,
    public router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.especieId = this.route.snapshot.params["especie_id"]
      ? +this.route.snapshot.params["especie_id"]
      : null;
    this.isEdit = !!this.especieId;
    if (this.isEdit && this.especieId) {
      this.especieService
        .buscar(this.estudoId, this.especieId)
        .subscribe((e) => {
          this.classe = e.classe;
          this.ordem = e.ordem;
          this.familia = e.familia;
          this.genero = e.genero;
          this.especie = e.especie;
          this.nomePopular = e.nome_popular;
          this.statusConservacao = e.status_conservacao;
          this.endemismo = e.endemismo;
          if (e.foto)
            this.fotoPreview = this.util.buildFotoUrl(this.apiUrl, e.foto);
        });
    }
  }

  abrirArquivo() {
    this.util.openFilePicker((b64, name) => {
      this.fotoBase64 = b64;
      this.fotoNome = name;
      this.fotoPreview = b64;
    });
  }
  abrirCamera() {
    this.util.openCamera((b64) => {
      this.fotoBase64 = b64;
      this.fotoNome = "foto.jpg";
      this.fotoPreview = b64;
    });
  }

  salvar() {
    if (!this.classe || !this.genero || !this.especie) {
      this.erro = "Preencha Classe, Gênero e Espécie.";
      return;
    }
    this.loading = true;
    this.erro = "";
    const base = {
      classe: this.classe,
      ordem: this.ordem,
      familia: this.familia,
      genero: this.genero,
      especie: this.especie,
      nome_popular: this.nomePopular,
      status_conservacao: this.statusConservacao,
      endemismo: this.endemismo,
      ...(this.fotoBase64 ? { foto: this.fotoBase64 } : {}),
    };
    const obs =
      this.isEdit && this.especieId
        ? this.especieService.atualizar(this.estudoId, this.especieId, base)
        : this.especieService.criar(this.estudoId, base);
    obs.subscribe({
      next: (e) =>
        this.router.navigate(["/estudos", this.estudoId, "especies", e.id]),
      error: (err) => {
        this.erro = extrairMensagemErro(err, "Erro ao salvar.");
        this.loading = false;
      },
    });
  }

  voltar() {
    this.router.navigate(["/estudos", this.estudoId, "especies"]);
  }
}
