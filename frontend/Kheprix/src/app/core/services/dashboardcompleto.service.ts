import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { EstudoDashboard } from '../../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardCompletoService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  getDashboard(estudoId: number): Observable<EstudoDashboard> {
    return this.get<EstudoDashboard>(`/estudos/${estudoId}/dashboard`);
  }
}