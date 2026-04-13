import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { Convite, ConviteRecebido, ConvitePublico, StatusConvite } from '../../models';

@Injectable({ providedIn: 'root' })
export class ConviteService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  criar(estudo_id: number, email_convidado: string): Observable<Convite> {
    return this.post<Convite>(`/estudos/${estudo_id}/convites`, { email_convidado });
  }

  listar(estudo_id: number, status?: StatusConvite): Observable<Convite[]> {
    const params: Record<string, string> = {};
    if (status) params['status'] = status;
    return this.get<Convite[]>(`/estudos/${estudo_id}/convites`, params);
  }

  deletar(estudo_id: number, id: number): Observable<void> {
    return this.delete(`/estudos/${estudo_id}/convites/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class ConviteRecebidoService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  listar(): Observable<ConviteRecebido[]> {
    return this.get<ConviteRecebido[]>('/convites');
  }

  buscarPorToken(token: string): Observable<ConvitePublico> {
    return this.get<ConvitePublico>(`/convites/${token}`);
  }

  aceitar(token: string): Observable<{ mensagem: string }> {
    return this.post<{ mensagem: string }>(`/convites/${token}/aceitar`, {});
  }

  recusar(token: string): Observable<{ mensagem: string }> {
    return this.post<{ mensagem: string }>(`/convites/${token}/recusar`, {});
  }
}
