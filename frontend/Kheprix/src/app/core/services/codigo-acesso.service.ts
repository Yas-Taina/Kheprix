import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { BaseService } from "./base.service";
import {
  CodigoAcesso,
  AutocadastroEstudoRequest,
  AutocadastroEstudoResponse,
} from "../../models";

@Injectable({ providedIn: "root" })
export class CodigoAcessoService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  buscar(estudo_id: number): Observable<CodigoAcesso> {
    return this.get<CodigoAcesso>(`/estudos/${estudo_id}/codigo_acesso`);
  }

  atualizar(
    estudo_id: number,
    senha_autocadastro: string,
  ): Observable<CodigoAcesso> {
    return this.patch<CodigoAcesso>(`/estudos/${estudo_id}/codigo_acesso`, {
      senha_autocadastro,
    });
  }

  ingressar(
    data: AutocadastroEstudoRequest,
  ): Observable<AutocadastroEstudoResponse> {
    return this.post<AutocadastroEstudoResponse>("/estudos/ingressar", data);
  }
}
