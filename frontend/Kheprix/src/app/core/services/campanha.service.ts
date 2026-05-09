import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { BaseService } from "./base.service";
import { Campanha, CampanhaCreate, CampanhaUpdate } from "../../models";

@Injectable({ providedIn: "root" })
export class CampanhaService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  listar(estudo_id: number): Observable<Campanha[]> {
    return this.get<Campanha[]>(`/estudos/${estudo_id}/campanhas`);
  }

  buscar(estudo_id: number, id: number): Observable<Campanha> {
    return this.get<Campanha>(`/estudos/${estudo_id}/campanhas/${id}`);
  }

  criar(estudo_id: number, data: CampanhaCreate): Observable<Campanha> {
    return this.post<Campanha>(`/estudos/${estudo_id}/campanhas`, data);
  }

  atualizar(
    estudo_id: number,
    id: number,
    data: CampanhaUpdate,
  ): Observable<Campanha> {
    return this.patch<Campanha>(`/estudos/${estudo_id}/campanhas/${id}`, data);
  }

  deletar(estudo_id: number, id: number): Observable<void> {
    return this.delete(`/estudos/${estudo_id}/campanhas/${id}`);
  }
}
