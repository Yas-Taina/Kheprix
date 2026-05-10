import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { BaseService } from "./base.service";
import { Especie, EspecieCreate, EspecieUpdate } from "../../models";

@Injectable({ providedIn: "root" })
export class EspecieService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  listar(estudo_id: number, nome_popular?: string): Observable<Especie[]> {
    const params: Record<string, string> = {};
    if (nome_popular) params["nome_popular"] = nome_popular;
    return this.get<Especie[]>(`/estudos/${estudo_id}/especies`, params);
  }

  buscar(estudo_id: number, id: number): Observable<Especie> {
    return this.get<Especie>(`/estudos/${estudo_id}/especies/${id}`);
  }

  criar(estudo_id: number, data: EspecieCreate): Observable<Especie> {
    return this.post<Especie>(`/estudos/${estudo_id}/especies`, data);
  }

  atualizar(
    estudo_id: number,
    id: number,
    data: EspecieUpdate,
  ): Observable<Especie> {
    return this.patch<Especie>(`/estudos/${estudo_id}/especies/${id}`, data);
  }

  deletar(estudo_id: number, id: number): Observable<void> {
    return this.delete(`/estudos/${estudo_id}/especies/${id}`);
  }
}
