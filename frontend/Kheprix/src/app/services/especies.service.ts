import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import {
  Especie,
  CriarEspeciePayload,
  AtualizarEspeciePayload,
} from './modelos/especie.model';

@Injectable({
  providedIn: 'root',
})
export class EspeciesService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  // GET /estudos/:estudo_id/especies
  listar(estudoId: number, nomePopular?: string): Observable<Especie[]> {
    return this.get<Especie[]>(`/estudos/${estudoId}/especies`, {
      nome_popular: nomePopular,
    });
  }

  // GET /estudos/:estudo_id/especies/:id
  buscarPorId(estudoId: number, id: number): Observable<Especie> {
    return this.get<Especie>(`/estudos/${estudoId}/especies/${id}`);
  }

  // POST /estudos/:estudo_id/especies
  criar(estudoId: number, payload: CriarEspeciePayload): Observable<Especie> {
    return this.post<Especie>(`/estudos/${estudoId}/especies`, payload);
  }

  // PATCH /estudos/:estudo_id/especies/:id
  atualizar(estudoId: number, id: number, payload: AtualizarEspeciePayload): Observable<Especie> {
    return this.patch<Especie>(`/estudos/${estudoId}/especies/${id}`, payload);
  }

  // DELETE /estudos/:estudo_id/especies/:id
  deletar(estudoId: number, id: number): Observable<void> {
    return this.delete<void>(`/estudos/${estudoId}/especies/${id}`);
  }
}
