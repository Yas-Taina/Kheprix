import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { UnidadeAmostral, UnidadeAmostralCreate, UnidadeAmostralUpdate } from '../../models';

@Injectable({ providedIn: 'root' })
export class UnidadeAmostralService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  listar(estudo_id: number, campanha_id: number): Observable<UnidadeAmostral[]> {
    return this.get<UnidadeAmostral[]>(
      `/estudos/${estudo_id}/campanhas/${campanha_id}/unidades_amostrais`
    );
  }

  buscar(estudo_id: number, campanha_id: number, id: number): Observable<UnidadeAmostral> {
    return this.get<UnidadeAmostral>(
      `/estudos/${estudo_id}/campanhas/${campanha_id}/unidades_amostrais/${id}`
    );
  }

  criar(estudo_id: number, campanha_id: number, data: UnidadeAmostralCreate): Observable<UnidadeAmostral> {
    return this.post<UnidadeAmostral>(
      `/estudos/${estudo_id}/campanhas/${campanha_id}/unidades_amostrais`,
      data
    );
  }

  atualizar(estudo_id: number, campanha_id: number, id: number, data: UnidadeAmostralUpdate): Observable<UnidadeAmostral> {
    return this.patch<UnidadeAmostral>(
      `/estudos/${estudo_id}/campanhas/${campanha_id}/unidades_amostrais/${id}`,
      data
    );
  }

  deletar(estudo_id: number, campanha_id: number, id: number): Observable<void> {
    return this.delete(
      `/estudos/${estudo_id}/campanhas/${campanha_id}/unidades_amostrais/${id}`
    );
  }
}
