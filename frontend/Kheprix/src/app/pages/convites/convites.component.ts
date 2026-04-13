import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConviteRecebidoService } from '../../core/services/convite.service';
import { CodigoAcessoService } from '../../core/services/codigo-acesso.service';
import { ConviteRecebido } from '../../models';
import { UtilService } from '../../core/services/util.service';

@Component({
  selector: 'app-convites',
  standalone: true,
  templateUrl: './convites.component.html',
  styleUrls: ['./convites.component.css'],
  imports: [CommonModule, FormsModule],
})
export class ConvitesComponent implements OnInit {
  convites: ConviteRecebido[] = [];
  loading = true;
  msgConvite = ''; erroConvite = '';
  codigoAcesso = ''; senhaAcesso = '';
  loadingAcesso = false; erroAcesso = ''; msgAcesso = '';

  constructor(
    private conviteService: ConviteRecebidoService,
    private codigoService: CodigoAcessoService,
    private router: Router,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.conviteService.listar().subscribe({
      next: (c) => { this.convites = c; this.loading = false; },
      error: () => this.loading = false,
    });
  }

  aceitar(c: ConviteRecebido) {
    // Os convites recebidos precisam de token — buscamos pelo id do convite
    // Aqui usamos o id como identificador para construir ação
    this.conviteService.aceitar(String(c.id)).subscribe({
      next: (r) => { this.msgConvite = r.mensagem; this.convites = this.convites.filter(x => x.id !== c.id); },
      error: () => this.erroConvite = 'Erro ao aceitar convite.',
    });
  }

  recusar(c: ConviteRecebido) {
    this.conviteService.recusar(String(c.id)).subscribe({
      next: (r) => { this.msgConvite = r.mensagem; this.convites = this.convites.filter(x => x.id !== c.id); },
      error: () => this.erroConvite = 'Erro ao recusar convite.',
    });
  }

  ingressarEstudo() {
    if (!this.codigoAcesso || !this.senhaAcesso) { this.erroAcesso = 'Preencha código e senha.'; return; }
    this.loadingAcesso = true; this.erroAcesso = '';
    this.codigoService.ingressar({ codigo: this.codigoAcesso, senha_autocadastro: this.senhaAcesso }).subscribe({
      next: (r) => { this.msgAcesso = `Ingressou em "${r.nome_estudo}" como ${r.perfil}.`; this.loadingAcesso = false; this.codigoAcesso = ''; this.senhaAcesso = ''; },
      error: () => { this.erroAcesso = 'Código ou senha inválidos.'; this.loadingAcesso = false; },
    });
  }

  lerQrCode() {
    // PLACEHOLDER: Integrar biblioteca de leitura de QR Code (ex: ngx-scanner-qrcode)
    // Após leitura, preencher this.codigoAcesso com o resultado
    alert('PLACEHOLDER: Integrar leitor de QR Code.\nSubstituir este alert pela abertura do scanner de câmera.\nBiblioteca sugerida: ngx-scanner-qrcode');
  }
}
