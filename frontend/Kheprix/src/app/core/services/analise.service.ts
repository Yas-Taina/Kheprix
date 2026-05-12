import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { BaseService } from "./base.service";
import {
  ExecutarAnaliseRequest,
  ExecutarAnaliseResponse,
} from "../../models/analise.model";

@Injectable({ providedIn: "root" })
export class AnaliseService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  executar(
    estudoId: number,
    payload: ExecutarAnaliseRequest,
  ): Observable<ExecutarAnaliseResponse> {
    return this.post<ExecutarAnaliseResponse>(
      `/estudos/${estudoId}/analises/executar`,
      payload,
    );
  }

  downloadArquivo(
    estudoId: number,
    chave: string,
    arquivo: string,
  ): Observable<Blob> {
    return this.getBlob(`/analises/estudos/${estudoId}/${chave}/${arquivo}`);
  }

  downloadPorUrl(urlArquivo: string): Observable<Blob> {
    const path = urlArquivo.startsWith(this.apiUrl)
      ? urlArquivo.slice(this.apiUrl.length)
      : urlArquivo;
    return this.getBlob(path);
  }
}
