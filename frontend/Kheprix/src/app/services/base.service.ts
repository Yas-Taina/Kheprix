import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class BaseService {
  protected readonly apiUrl = 'http://localhost:3000';

  constructor(protected http: HttpClient) {}

  protected getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('auth_token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    });
  }

  protected get<T>(endpoint: string, params?: Record<string, string | undefined>): Observable<T> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          httpParams = httpParams.set(key, value);
        }
      });
    }

    return this.http.get<T>(`${this.apiUrl}${endpoint}`, {
      headers: this.getAuthHeaders(),
      params: httpParams,
    });
  }

  protected post<T>(endpoint: string, body: unknown = {}): Observable<T> {
    return this.http.post<T>(`${this.apiUrl}${endpoint}`, body, {
      headers: this.getAuthHeaders(),
    });
  }

  protected patch<T>(endpoint: string, body: unknown = {}): Observable<T> {
    return this.http.patch<T>(`${this.apiUrl}${endpoint}`, body, {
      headers: this.getAuthHeaders(),
    });
  }

  protected delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(`${this.apiUrl}${endpoint}`, {
      headers: this.getAuthHeaders(),
    });
  }
}
