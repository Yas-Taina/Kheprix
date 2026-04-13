import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { Dashboard } from '../../models';

@Injectable({ providedIn: 'root' })
export class DashboardService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  getDashboard(): Observable<Dashboard> {
    return this.get<Dashboard>('/dashboard');
  }
}
