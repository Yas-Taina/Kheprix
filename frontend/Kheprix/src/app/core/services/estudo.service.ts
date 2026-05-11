import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { BaseService } from "./base.service";
import {
  Estudo,
  EstudoCreate,
  EstudoUpdate,
  TipoAgrupamento,
  FormatoExportacao,
} from "../../models";

@Injectable({ providedIn: "root" })
export class EstudoService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  listar(filtros?: {
    nome?: string;
    criado_a_partir_de?: string;
    criado_ate?: string;
    atualizado_a_partir_de?: string;
    atualizado_ate?: string;
  }): Observable<Estudo[]> {
    return this.get<Estudo[]>("/estudos", filtros as Record<string, string>);
  }

  criar(data: EstudoCreate): Observable<Estudo> {
    return this.post<Estudo>("/estudos", data);
  }

  atualizar(id: number, data: EstudoUpdate): Observable<Estudo> {
    return this.patch<Estudo>(`/estudos/${id}`, data);
  }

  deletar(id: number): Observable<void | { mensagem: string }> {
    return this.delete(`/estudos/${id}`);
  }

  exportarDados(
    id: number,
    agrupamento?: TipoAgrupamento,
    formato: FormatoExportacao = "csv",
  ): Observable<Blob> {
    let params = new HttpParams().set("formato", formato);
 
    if (formato !== "xml" && agrupamento) {
      params = params.set("agrupamento", agrupamento);
    }
 
    const acceptHeader = formato === "xml" ? "application/xml" : "text/csv";
 
    return this.http
      .get(`${this.apiUrl}/estudos/${id}/exportar_dados`, {
        headers: this.getHeadersBlob(acceptHeader),
        params,
        responseType: "blob",
      })
      .pipe(
        catchError((e) => {
          console.error("API Error:", e);
          throw e;
        }),
      );
  }
}
