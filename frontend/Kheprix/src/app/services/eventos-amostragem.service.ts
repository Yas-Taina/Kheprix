import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import {
  EventoAmostragem,
  CriarEventoAmostragemPayload,
  AtualizarEventoAmostragemPayload,
} from './modelos/evento-amostragem.model';

@Injectable({
  providedIn: 'root',
})
export class EventosAmostragemService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  private base(estudoId: number, campanhaId: number, unidadeAmostralId: number): string {
    return `/estudos/${estudoId}/campanhas/${campanhaId}/unidades_amostrais/${unidadeAmostralId}/eventos_amostragem`;
  }

  // GET .../eventos_amostragem
  listar(
    estudoId: number,
    campanhaId: number,
    unidadeAmostralId: number
  ): Observable<EventoAmostragem[]> {
    return this.get<EventoAmostragem[]>(this.base(estudoId, campanhaId, unidadeAmostralId));
  }

  // GET .../eventos_amostragem/:id
  buscarPorId(
    estudoId: number,
    campanhaId: number,
    unidadeAmostralId: number,
    id: number
  ): Observable<EventoAmostragem> {
    return this.get<EventoAmostragem>(`${this.base(estudoId, campanhaId, unidadeAmostralId)}/${id}`);
  }

  // POST .../eventos_amostragem
  criar(
    estudoId: number,
    campanhaId: number,
    unidadeAmostralId: number,
    payload: CriarEventoAmostragemPayload
  ): Observable<EventoAmostragem> {
    return this.post<EventoAmostragem>(
      this.base(estudoId, campanhaId, unidadeAmostralId),
      payload
    );
  }

  // PATCH .../eventos_amostragem/:id
  atualizar(
    estudoId: number,
    campanhaId: number,
    unidadeAmostralId: number,
    id: number,
    payload: AtualizarEventoAmostragemPayload
  ): Observable<EventoAmostragem> {
    return this.patch<EventoAmostragem>(
      `${this.base(estudoId, campanhaId, unidadeAmostralId)}/${id}`,
      payload
    );
  }

  // DELETE .../eventos_amostragem/:id
  deletar(
    estudoId: number,
    campanhaId: number,
    unidadeAmostralId: number,
    id: number
  ): Observable<void> {
    return this.delete<void>(`${this.base(estudoId, campanhaId, unidadeAmostralId)}/${id}`);
  }
}
