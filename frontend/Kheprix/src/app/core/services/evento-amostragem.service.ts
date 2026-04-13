import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { EventoAmostragem, EventoAmostragemCreate, EventoAmostragemUpdate } from '../../models';

@Injectable({ providedIn: 'root' })
export class EventoAmostragemService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  private base(eid: number, cid: number, uid: number): string {
    return `/estudos/${eid}/campanhas/${cid}/unidades_amostrais/${uid}/eventos_amostragem`;
  }

  listar(estudo_id: number, campanha_id: number, unidade_id: number): Observable<EventoAmostragem[]> {
    return this.get<EventoAmostragem[]>(this.base(estudo_id, campanha_id, unidade_id));
  }

  buscar(estudo_id: number, campanha_id: number, unidade_id: number, id: number): Observable<EventoAmostragem> {
    return this.get<EventoAmostragem>(`${this.base(estudo_id, campanha_id, unidade_id)}/${id}`);
  }

  criar(estudo_id: number, campanha_id: number, unidade_id: number, data: EventoAmostragemCreate): Observable<EventoAmostragem> {
    return this.post<EventoAmostragem>(this.base(estudo_id, campanha_id, unidade_id), data);
  }

  atualizar(estudo_id: number, campanha_id: number, unidade_id: number, id: number, data: EventoAmostragemUpdate): Observable<EventoAmostragem> {
    return this.patch<EventoAmostragem>(
      `${this.base(estudo_id, campanha_id, unidade_id)}/${id}`,
      data
    );
  }

  deletar(estudo_id: number, campanha_id: number, unidade_id: number, id: number): Observable<void> {
    return this.delete(`${this.base(estudo_id, campanha_id, unidade_id)}/${id}`);
  }
}
