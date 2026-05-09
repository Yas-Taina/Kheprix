import {
  Component,
  OnInit,
  AfterViewInit,
  ViewChild,
  ElementRef,
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ColaboradorService } from "../../../core/services/colaborador.service";
import { ConviteService } from "../../../core/services/convite.service";
import { CodigoAcessoService } from "../../../core/services/codigo-acesso.service";
import { Colaborador, CodigoAcesso, PerfilColaborador } from "../../../models";
import QRCode from "qrcode";

@Component({
  selector: "app-colaboradores",
  standalone: true,
  templateUrl: "./colaboradores.component.html",
  styleUrls: ["./colaboradores.component.css"],
  imports: [CommonModule, FormsModule],
})
export class ColaboradoresComponent implements OnInit, AfterViewInit {
  @ViewChild("qrCanvas") qrCanvas!: ElementRef<HTMLCanvasElement>;

  estudoId!: number;
  colaboradores: Colaborador[] = [];
  codigoAcesso: CodigoAcesso | null = null;
  emailConvite = "";
  novaSenhaAcesso = "";
  loading = false;
  loadingConvite = false;
  loadingCodigo = true;
  loadingSenha = false;
  erroConvite = "";
  msgConvite = "";

  constructor(
    private colabService: ColaboradorService,
    private conviteService: ConviteService,
    private codigoService: CodigoAcessoService,
    public router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.estudoId = +this.route.snapshot.params["estudo_id"];
    this.colabService
      .listar(this.estudoId)
      .subscribe((c) => (this.colaboradores = c));
    this.codigoService.buscar(this.estudoId).subscribe({
      next: (c) => {
        this.codigoAcesso = c;
        this.loadingCodigo = false;
        setTimeout(() => this.gerarQrCode(), 100);
      },
      error: () => (this.loadingCodigo = false),
    });
  }

  ngAfterViewInit() {
    if (this.codigoAcesso) this.gerarQrCode();
  }

  gerarQrCode() {
    if (!this.qrCanvas || !this.codigoAcesso?.codigo) return;

    const canvas = this.qrCanvas.nativeElement;
    const valor = this.codigoAcesso.codigo;

    QRCode.toCanvas(canvas, valor, {
      width: 150,
      margin: 2,
      color: {
        dark: "#333333",
        light: "#FFFFFF",
      },
    }).catch((err) => console.error(err));
  }

  convidar() {
    if (!this.emailConvite) {
      this.erroConvite = "Informe o e-mail.";
      return;
    }
    this.loadingConvite = true;
    this.erroConvite = "";
    this.conviteService.criar(this.estudoId, this.emailConvite).subscribe({
      next: () => {
        this.msgConvite = "Convite enviado!";
        this.emailConvite = "";
        this.loadingConvite = false;
      },
      error: () => {
        this.erroConvite = "Erro ao enviar convite.";
        this.loadingConvite = false;
      },
    });
  }

  alterarAcesso(c: Colaborador) {
    this.colabService
      .atualizar(this.estudoId, c.id_usuario, { perfil: c.perfil })
      .subscribe();
  }

  remover(c: Colaborador) {
    if (!confirm(`Remover "${c.nome}"?`)) return;
    this.colabService.deletar(this.estudoId, c.id_usuario).subscribe({
      next: () =>
        (this.colaboradores = this.colaboradores.filter(
          (x) => x.id_usuario !== c.id_usuario,
        )),
    });
  }

  alterarSenha() {
    if (!this.novaSenhaAcesso) return;
    this.loadingSenha = true;
    this.codigoService
      .atualizar(this.estudoId, this.novaSenhaAcesso)
      .subscribe({
        next: (c) => {
          this.codigoAcesso = c;
          this.novaSenhaAcesso = "";
          this.loadingSenha = false;
        },
        error: () => (this.loadingSenha = false),
      });
  }
}
