import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import {
  EstudoListItem,
  EstudoFiltros,
  CriarEstudoPayload,
  EstudoCriadoResponse,
} from './modelos/estudo.model';

@Injectable({
  providedIn: 'root',
})
export class EstudosService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  // GET /estudos
  listar(filtros?: EstudoFiltros): Observable<EstudoListItem[]> {
    return this.get<EstudoListItem[]>('/estudos', filtros as Record<string, string | undefined>);
  }

  // POST /estudos
  criar(payload: CriarEstudoPayload): Observable<EstudoCriadoResponse> {
    return this.post<EstudoCriadoResponse>('/estudos', payload);
  }

  // DELETE /estudos/:id
  deletar(id: number): Observable<void | { mensagem: string }> {
    return this.delete<void | { mensagem: string }>(`/estudos/${id}`);
  }
}
