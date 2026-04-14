import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { Colaborador, ColaboradorUpdate } from '../../models';

@Injectable({ providedIn: 'root' })
export class ColaboradorService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  listar(estudo_id: number): Observable<Colaborador[]> {
    return this.get<Colaborador[]>(`/estudos/${estudo_id}/colaboradores`);
  }

  atualizar(estudo_id: number, id: number, data: ColaboradorUpdate): Observable<Colaborador> {
    return this.patch<Colaborador>(`/estudos/${estudo_id}/colaboradores/${id}`, data);
  }

  deletar(estudo_id: number, id: number): Observable<void> {
    return this.delete(`/estudos/${estudo_id}/colaboradores/${id}`);
  }
}
