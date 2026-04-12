import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import {
  UnidadeAmostral,
  CriarUnidadeAmostralPayload,
  AtualizarUnidadeAmostralPayload,
} from './modelos/unidade-amostral.model';

@Injectable({
  providedIn: 'root',
})
export class UnidadesAmostraisService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  private base(estudoId: number, campanhaId: number): string {
    return `/estudos/${estudoId}/campanhas/${campanhaId}/unidades_amostrais`;
  }

  // GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais
  listar(estudoId: number, campanhaId: number): Observable<UnidadeAmostral[]> {
    return this.get<UnidadeAmostral[]>(this.base(estudoId, campanhaId));
  }

  // GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id
  buscarPorId(estudoId: number, campanhaId: number, id: number): Observable<UnidadeAmostral> {
    return this.get<UnidadeAmostral>(`${this.base(estudoId, campanhaId)}/${id}`);
  }

  // POST /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais
  criar(
    estudoId: number,
    campanhaId: number,
    payload: CriarUnidadeAmostralPayload
  ): Observable<UnidadeAmostral> {
    return this.post<UnidadeAmostral>(this.base(estudoId, campanhaId), payload);
  }

  // PATCH /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id
  atualizar(
    estudoId: number,
    campanhaId: number,
    id: number,
    payload: AtualizarUnidadeAmostralPayload
  ): Observable<UnidadeAmostral> {
    return this.patch<UnidadeAmostral>(`${this.base(estudoId, campanhaId)}/${id}`, payload);
  }

  // DELETE /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id
  deletar(estudoId: number, campanhaId: number, id: number): Observable<void> {
    return this.delete<void>(`${this.base(estudoId, campanhaId)}/${id}`);
  }
}
