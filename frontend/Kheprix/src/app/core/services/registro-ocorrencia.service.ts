import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { BaseService } from "./base.service";
import {
  RegistroOcorrencia,
  RegistroOcorrenciaCreate,
  RegistroOcorrenciaUpdate,
} from "../../models";

@Injectable({ providedIn: "root" })
export class RegistroOcorrenciaService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  private base(eid: number, cid: number, uid: number, evid: number): string {
    return `/estudos/${eid}/campanhas/${cid}/unidades_amostrais/${uid}/eventos_amostragem/${evid}/registro_ocorrencias`;
  }

  listar(
    estudo_id: number,
    campanha_id: number,
    unidade_id: number,
    evento_id: number,
  ): Observable<RegistroOcorrencia[]> {
    return this.get<RegistroOcorrencia[]>(
      this.base(estudo_id, campanha_id, unidade_id, evento_id),
    );
  }

  buscar(
    estudo_id: number,
    campanha_id: number,
    unidade_id: number,
    evento_id: number,
    id: number,
  ): Observable<RegistroOcorrencia> {
    return this.get<RegistroOcorrencia>(
      `${this.base(estudo_id, campanha_id, unidade_id, evento_id)}/${id}`,
    );
  }

  criar(
    estudo_id: number,
    campanha_id: number,
    unidade_id: number,
    evento_id: number,
    data: RegistroOcorrenciaCreate,
  ): Observable<RegistroOcorrencia> {
    return this.post<RegistroOcorrencia>(
      this.base(estudo_id, campanha_id, unidade_id, evento_id),
      data,
    );
  }

  atualizar(
    estudo_id: number,
    campanha_id: number,
    unidade_id: number,
    evento_id: number,
    id: number,
    data: RegistroOcorrenciaUpdate,
  ): Observable<RegistroOcorrencia> {
    return this.patch<RegistroOcorrencia>(
      `${this.base(estudo_id, campanha_id, unidade_id, evento_id)}/${id}`,
      data,
    );
  }

  deletar(
    estudo_id: number,
    campanha_id: number,
    unidade_id: number,
    evento_id: number,
    id: number,
  ): Observable<void> {
    return this.delete(
      `${this.base(estudo_id, campanha_id, unidade_id, evento_id)}/${id}`,
    );
  }
}
