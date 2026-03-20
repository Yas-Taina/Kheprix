import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import {
  UnidadeAmostral,
  CriarUnidadeAmostralRequest,
  AtualizarUnidadeAmostralRequest,
} from './models/unidade-amostral.model';

@Injectable({ providedIn: 'root' })
export class UnidadesAmostraisService extends BaseService {

  listar(estudoId: number, campanhaId: number): Observable<UnidadeAmostral[]> {
    return this.get<UnidadeAmostral[]>(`/estudos/${estudoId}/campanhas/${campanhaId}/unidades_amostrais`);
  }

  buscarPorId(estudoId: number, campanhaId: number, id: number): Observable<UnidadeAmostral> {
    return this.get<UnidadeAmostral>(`/estudos/${estudoId}/campanhas/${campanhaId}/unidades_amostrais/${id}`);
  }

  criar(estudoId: number, campanhaId: number, body: CriarUnidadeAmostralRequest): Observable<UnidadeAmostral> {
    return this.post<UnidadeAmostral>(`/estudos/${estudoId}/campanhas/${campanhaId}/unidades_amostrais`, body);
  }

  atualizar(estudoId: number, campanhaId: number, id: number, body: AtualizarUnidadeAmostralRequest): Observable<UnidadeAmostral> {
    return this.patch<UnidadeAmostral>(`/estudos/${estudoId}/campanhas/${campanhaId}/unidades_amostrais/${id}`, body);
  }

  deletar(estudoId: number, campanhaId: number, id: number): Observable<void> {
    return this.delete<void>(`/estudos/${estudoId}/campanhas/${campanhaId}/unidades_amostrais/${id}`);
  }
}
