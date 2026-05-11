import { Component, OnInit, ViewChild, ElementRef } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { ConviteRecebidoService } from "../../core/services/convite.service";
import { CodigoAcessoService } from "../../core/services/codigo-acesso.service";
import { ConviteRecebido } from "../../models";
import { UtilService } from "../../core/services/util.service";
import { BrowserMultiFormatReader, IScannerControls } from "@zxing/browser";

@Component({
  selector: "app-convites",
  standalone: true,
  templateUrl: "./convites.component.html",
  styleUrls: ["./convites.component.css"],
  imports: [CommonModule, FormsModule],
})
export class ConvitesComponent implements OnInit {
  convites: ConviteRecebido[] = [];
  loading = true;
  msgConvite = "";
  erroConvite = "";
  codigoAcesso = "";
  senhaAcesso = "";
  loadingAcesso = false;
  erroAcesso = "";
  msgAcesso = "";

  @ViewChild("video") video!: ElementRef<HTMLVideoElement>;

  scannerAtivo = false;
  private codeReader = new BrowserMultiFormatReader();
  private controls: IScannerControls | null = null;
  private monitorInterval: any;

  constructor(
    private conviteService: ConviteRecebidoService,
    private codigoService: CodigoAcessoService,
    private router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.conviteService.listar().subscribe({
      next: (c) => {
        this.convites = c;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  aceitar(c: ConviteRecebido) {
    this.conviteService.aceitar(String(c.token)).subscribe({
      next: (r) => {
        this.msgConvite = r.mensagem;
        this.convites = this.convites.filter((x) => x.id !== c.id);
      },
      error: () => (this.erroConvite = "Erro ao aceitar convite."),
    });
  }

  recusar(c: ConviteRecebido) {
    this.conviteService.recusar(String(c.token)).subscribe({
      next: (r) => {
        this.msgConvite = r.mensagem;
        this.convites = this.convites.filter((x) => x.id !== c.id);
      },
      error: () => (this.erroConvite = "Erro ao recusar convite."),
    });
  }

  ingressarEstudo() {
    if (!this.codigoAcesso || !this.senhaAcesso) {
      this.erroAcesso = "Preencha código e senha.";
      return;
    }

    this.loadingAcesso = true;
    this.erroAcesso = "";

    this.codigoService
      .ingressar({
        codigo: this.codigoAcesso,
        senha_autocadastro: this.senhaAcesso,
      })
      .subscribe({
        next: (r) => {
          this.msgAcesso = `Ingressou em "${r.nome_estudo}" como ${r.perfil}.`;
          this.loadingAcesso = false;
          this.codigoAcesso = "";
          this.senhaAcesso = "";
        },
        error: () => {
          this.erroAcesso = "Código ou senha inválidos.";
          this.loadingAcesso = false;
        },
      });
  }

  lerQrCode() {
    this.scannerAtivo = true;

    setTimeout(() => {
      if (this.video?.nativeElement) {
        this.iniciarScanner();
      }
    }, 100);
  }

  async iniciarScanner() {
    try {
      const devices = await BrowserMultiFormatReader.listVideoInputDevices();

      const device =
        devices.find(
          (d) =>
            d.label.toLowerCase().includes("back") ||
            d.label.toLowerCase().includes("traseira"),
        ) || devices[0];

      this.controls = await this.codeReader.decodeFromConstraints(
        {
          video: {
            deviceId: device?.deviceId,
            facingMode: "environment",
            width: { ideal: 640 },
            height: { ideal: 480 },
          },
        },
        this.video.nativeElement,
        (result) => {
          if (result) {
            this.codigoAcesso = result.getText();
            this.fecharScanner();
          }
        },
      );

      this.startMonitor();
    } catch (err) {
      console.error("Erro ao acessar câmera:", err);
    }
  }

  fecharScanner() {
    if (this.controls) {
      this.controls.stop();
      this.controls = null;
    }

    const videoEl = this.video?.nativeElement;
    if (videoEl) {
      videoEl.srcObject = null;
    }

    this.stopMonitor();
    this.scannerAtivo = false;
  }

  private startMonitor() {
    this.stopMonitor();

    this.monitorInterval = setInterval(() => {
      const videoEl = this.video?.nativeElement;

      if (!videoEl || videoEl.readyState < 2) {
        this.restartScanner();
      }
    }, 2000);
  }

  private stopMonitor() {
    if (this.monitorInterval) {
      clearInterval(this.monitorInterval);
      this.monitorInterval = null;
    }
  }

  private async restartScanner() {
    this.fecharScanner();
    await this.iniciarScanner();
  }
}
