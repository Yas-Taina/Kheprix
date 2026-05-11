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
import { Convite } from "../../../models";
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
  convites: Convite[] = [];
  emailConvite = "";
  novaSenhaAcesso = "";
  loading = false;
  loadingConvite = false;
  loadingCodigo = true;
  loadingSenha = false;
  loadingConvites = false;
  erroConvite = "";
  msgConvite = "";
  erroConvites = "";

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
    this.carregarConvites();
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

  carregarConvites() {
    this.loadingConvites = true;
    this.erroConvites = "";
    this.conviteService.listar(this.estudoId, "pendente").subscribe({
      next: (convites) => {
        this.convites = convites;
        this.loadingConvites = false;
      },
      error: () => {
        this.erroConvites = "Erro ao carregar convites.";
        this.loadingConvites = false;
      },
    });
  }

  cancelarConvite(c: Convite) {
    if (!confirm(`Cancelar convite para "${c.email_convidado}"?`)) return;
    this.conviteService.deletar(this.estudoId, c.id).subscribe({
      next: () => {
        this.convites = this.convites.filter((x) => x.id !== c.id);
      },
      error: () => {
        this.erroConvites = "Erro ao cancelar convite.";
      },
    });
  }

  convidar() {
    if (!this.emailConvite) {
      this.erroConvite = "Informe o e-mail.";
      return;
    }
    this.loadingConvite = true;
    this.erroConvite = "";
    this.conviteService.criar(this.estudoId, this.emailConvite).subscribe({
      next: (novoConvite) => {
        this.msgConvite = "Convite enviado!";
        this.emailConvite = "";
        this.loadingConvite = false;
        this.convites = [...this.convites, novoConvite];
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
