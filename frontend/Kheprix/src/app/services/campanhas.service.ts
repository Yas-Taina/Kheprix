import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import {
  Campanha,
  CriarCampanhaRequest,
  AtualizarCampanhaRequest,
} from './models/campanha.model';

@Injectable({ providedIn: 'root' })
export class CampanhasService extends BaseService {

  listar(estudoId: number): Observable<Campanha[]> {
    return this.get<Campanha[]>(`/estudos/${estudoId}/campanhas`);
  }

  buscarPorId(estudoId: number, id: number): Observable<Campanha> {
    return this.get<Campanha>(`/estudos/${estudoId}/campanhas/${id}`);
  }

  criar(estudoId: number, body: CriarCampanhaRequest): Observable<Campanha> {
    return this.post<Campanha>(`/estudos/${estudoId}/campanhas`, body);
  }

  atualizar(estudoId: number, id: number, body: AtualizarCampanhaRequest): Observable<Campanha> {
    return this.patch<Campanha>(`/estudos/${estudoId}/campanhas/${id}`, body);
  }

  deletar(estudoId: number, id: number): Observable<void> {
    return this.delete<void>(`/estudos/${estudoId}/campanhas/${id}`);
  }
}
