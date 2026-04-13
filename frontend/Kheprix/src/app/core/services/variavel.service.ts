import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { Variavel, NivelAplicacao } from '../../models';

@Injectable({ providedIn: 'root' })
export class VariavelService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  listar(estudo_id: number, nivel_aplicacao?: NivelAplicacao): Observable<Variavel[]> {
    const params: Record<string, string> = {};
    if (nivel_aplicacao) params['nivel_aplicacao'] = nivel_aplicacao;
    return this.get<Variavel[]>(`/estudos/${estudo_id}/variaveis`, params);
  }
}
