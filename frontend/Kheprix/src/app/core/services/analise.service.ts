import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import {
  ExecutarAnaliseRequest,
  ExecutarAnaliseResponse,
} from '../../models/analise.model';

@Injectable({ providedIn: 'root' })
export class AnaliseService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  /**
   * Executa uma análise estatística.
   * POST /estudos/:estudo_id/analises/executar
   */
  executar(
    estudoId: number,
    payload: ExecutarAnaliseRequest
  ): Observable<ExecutarAnaliseResponse> {
    return this.post<ExecutarAnaliseResponse>(
      `/estudos/${estudoId}/analises/executar`,
      payload
    );
  }

  /**
   * Download do resultado exportado (ZIP com JSON, XML e, se houver, HTML do gráfico).
   * GET /analises/estudos/:estudo_id/:chave/:arquivo
   */
  downloadArquivo(
    estudoId: number,
    chave: string,
    arquivo: string
  ): Observable<Blob> {
    return this.getBlob(
      `/analises/estudos/${estudoId}/${chave}/${arquivo}`
    );
  }
}