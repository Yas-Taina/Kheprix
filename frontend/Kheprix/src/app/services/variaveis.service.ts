import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import { Variavel, NivelAplicacao } from './modelos/variavel.model';

@Injectable({
  providedIn: 'root',
})
export class VariaveisService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  // GET /estudos/:estudo_id/variaveis
  listar(estudoId: number, nivelAplicacao?: NivelAplicacao): Observable<Variavel[]> {
    return this.get<Variavel[]>(`/estudos/${estudoId}/variaveis`, {
      nivel_aplicacao: nivelAplicacao,
    });
  }
}
