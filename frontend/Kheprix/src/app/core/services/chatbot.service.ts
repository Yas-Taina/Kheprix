import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { QueryResponse, InsightsResponse } from '../../models';

@Injectable({ providedIn: 'root' })
export class ChatbotService extends BaseService {
  constructor(http: HttpClient) {
    super(http);
  }

  pergunta(pergunta: string, estudo_ids: number[]): Observable<QueryResponse> {
    return this.post<QueryResponse>('/chatbot/query', { pergunta, estudo_ids });
  }

  insights(estudo_ids: number[]): Observable<InsightsResponse> {
    return this.post<InsightsResponse>('/chatbot/insights', { estudo_ids });
  }
}
