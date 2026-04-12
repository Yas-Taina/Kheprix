import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import {
  Colaborador,
  AtualizarColaboradorPayload,
  EnviarConvitePayload,
  ConviteEnviado,
  ConviteListItem,
  ConviteRecebidoListItem,
  ConviteRecebidoDetalhe,
  CodigoAcesso,
  AtualizarCodigoAcessoPayload,
  IngressarEstudoPayload,
  IngressarEstudoResponse,
} from './modelos/colaboracao.model';
import { MensagemResponse } from './modelos/autenticacao.model';

@Injectable({
  providedIn: 'root',
})
export class ColaboracaoService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  // ─── Colaboradores ─────────────────────────────────────────────

  // GET /estudos/:estudo_id/colaboradores
  listarColaboradores(estudoId: number): Observable<Colaborador[]> {
    return this.get<Colaborador[]>(`/estudos/${estudoId}/colaboradores`);
  }

  // PATCH /estudos/:estudo_id/colaboradores/:id
  atualizarColaborador(
    estudoId: number,
    id: number,
    payload: AtualizarColaboradorPayload
  ): Observable<Colaborador> {
    return this.patch<Colaborador>(`/estudos/${estudoId}/colaboradores/${id}`, payload);
  }

  // DELETE /estudos/:estudo_id/colaboradores/:id
  removerColaborador(estudoId: number, id: number): Observable<void> {
    return this.delete<void>(`/estudos/${estudoId}/colaboradores/${id}`);
  }

  // ─── Convites enviados (proprietário) ──────────────────────────

  // POST /estudos/:estudo_id/convites
  enviarConvite(estudoId: number, payload: EnviarConvitePayload): Observable<ConviteEnviado> {
    return this.post<ConviteEnviado>(`/estudos/${estudoId}/convites`, payload);
  }

  // GET /estudos/:estudo_id/convites
  listarConvitesEnviados(estudoId: number, status?: string): Observable<ConviteListItem[]> {
    return this.get<ConviteListItem[]>(`/estudos/${estudoId}/convites`, { status });
  }

  // DELETE /estudos/:estudo_id/convites/:id
  cancelarConvite(estudoId: number, id: number): Observable<void> {
    return this.delete<void>(`/estudos/${estudoId}/convites/${id}`);
  }

  // ─── Convites recebidos (autenticado / público) ────────────────

  // GET /convites
  listarConvitesRecebidos(): Observable<ConviteRecebidoListItem[]> {
    return this.get<ConviteRecebidoListItem[]>('/convites');
  }

  // GET /convites/:token  (público)
  buscarConvitePorToken(token: string): Observable<ConviteRecebidoDetalhe> {
    return this.get<ConviteRecebidoDetalhe>(`/convites/${token}`);
  }

  // POST /convites/:token/aceitar
  aceitarConvite(token: string): Observable<MensagemResponse> {
    return this.post<MensagemResponse>(`/convites/${token}/aceitar`);
  }

  // POST /convites/:token/recusar
  recusarConvite(token: string): Observable<MensagemResponse> {
    return this.post<MensagemResponse>(`/convites/${token}/recusar`);
  }

  // ─── Código de Acesso (proprietário) ──────────────────────────

  // GET /estudos/:estudo_id/codigo_acesso
  buscarCodigoAcesso(estudoId: number): Observable<CodigoAcesso> {
    return this.get<CodigoAcesso>(`/estudos/${estudoId}/codigo_acesso`);
  }

  // PATCH /estudos/:estudo_id/codigo_acesso
  atualizarCodigoAcesso(
    estudoId: number,
    payload: AtualizarCodigoAcessoPayload
  ): Observable<CodigoAcesso> {
    return this.patch<CodigoAcesso>(`/estudos/${estudoId}/codigo_acesso`, payload);
  }

  // ─── Autocadastro em Estudo ────────────────────────────────────

  // POST /estudos/ingressar
  ingressarEstudo(payload: IngressarEstudoPayload): Observable<IngressarEstudoResponse> {
    return this.post<IngressarEstudoResponse>('/estudos/ingressar', payload);
  }
}
