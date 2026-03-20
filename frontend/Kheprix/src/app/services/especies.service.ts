import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import {
  Especie,
  ListarEspeciesParams,
  CriarEspecieRequest,
  AtualizarEspecieRequest,
} from './models/especie.model';

@Injectable({ providedIn: 'root' })
export class EspeciesService extends BaseService {

  listar(estudoId: number, params?: ListarEspeciesParams): Observable<Especie[]> {
    const queryParams: Record<string, string> = {};
    if (params?.nome_popular) queryParams['nome_popular'] = params.nome_popular;
    if (params?.nome_cientifico) queryParams['nome_cientifico'] = params.nome_cientifico;
    return this.get<Especie[]>(`/estudos/${estudoId}/especies`, queryParams);
  }

  buscarPorId(estudoId: number, id: number): Observable<Especie> {
    return this.get<Especie>(`/estudos/${estudoId}/especies/${id}`);
  }

  criar(estudoId: number, body: CriarEspecieRequest): Observable<Especie> {
    return this.post<Especie>(`/estudos/${estudoId}/especies`, body);
  }

  atualizar(estudoId: number, id: number, body: AtualizarEspecieRequest): Observable<Especie> {
    return this.patch<Especie>(`/estudos/${estudoId}/especies/${id}`, body);
  }

  deletar(estudoId: number, id: number): Observable<void> {
    return this.delete<void>(`/estudos/${estudoId}/especies/${id}`);
  }
}
