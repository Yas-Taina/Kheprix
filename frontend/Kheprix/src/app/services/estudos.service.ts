import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import {
  EstudoResumo,
  ListarEstudosParams,
  CriarEstudoRequest,
  CriarEstudoResponse,
} from './models/estudo.model';

@Injectable({ providedIn: 'root' })
export class EstudosService extends BaseService {

  listar(params?: ListarEstudosParams): Observable<EstudoResumo[]> {
    const queryParams: Record<string, string> = {};
    if (params?.nome) queryParams['nome'] = params.nome;
    if (params?.criado_a_partir_de) queryParams['criado_a_partir_de'] = params.criado_a_partir_de;
    if (params?.criado_ate) queryParams['criado_ate'] = params.criado_ate;
    return this.get<EstudoResumo[]>('/estudos', queryParams);
  }

  criar(body: CriarEstudoRequest): Observable<CriarEstudoResponse> {
    return this.post<CriarEstudoResponse>('/estudos', body);
  }

  deletar(id: number): Observable<void> {
    return this.delete<void>(`/estudos/${id}`);
  }
}
