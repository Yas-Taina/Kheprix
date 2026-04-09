import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import {
  Campanha,
  CriarCampanhaPayload,
  AtualizarCampanhaPayload,
} from './modelos/campanha.model';

@Injectable({
  providedIn: 'root',
})
export class CampanhasService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  // GET /estudos/:estudo_id/campanhas
  listar(estudoId: number): Observable<Campanha[]> {
    return this.get<Campanha[]>(`/estudos/${estudoId}/campanhas`);
  }

  // GET /estudos/:estudo_id/campanhas/:id
  buscarPorId(estudoId: number, id: number): Observable<Campanha> {
    return this.get<Campanha>(`/estudos/${estudoId}/campanhas/${id}`);
  }

  // POST /estudos/:estudo_id/campanhas
  criar(estudoId: number, payload: CriarCampanhaPayload): Observable<Campanha> {
    return this.post<Campanha>(`/estudos/${estudoId}/campanhas`, payload);
  }

  // PATCH /estudos/:estudo_id/campanhas/:id
  atualizar(estudoId: number, id: number, payload: AtualizarCampanhaPayload): Observable<Campanha> {
    return this.patch<Campanha>(`/estudos/${estudoId}/campanhas/${id}`, payload);
  }

  // DELETE /estudos/:estudo_id/campanhas/:id
  deletar(estudoId: number, id: number): Observable<void> {
    return this.delete<void>(`/estudos/${estudoId}/campanhas/${id}`);
  }
}
