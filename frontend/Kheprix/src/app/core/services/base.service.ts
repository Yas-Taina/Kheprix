import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { Observable, throwError } from "rxjs";
import { catchError } from "rxjs/operators";
import { environment } from "../../../environments/environment";

@Injectable({
  providedIn: "root",
})
export class BaseService {
  protected apiUrl = environment.apiUrl;

  constructor(protected http: HttpClient) {}

  protected getHeaders(): HttpHeaders {
    const token = localStorage.getItem("kheprix_token");
    let headers = new HttpHeaders({ "Content-Type": "application/json" });
    if (token) {
      headers = headers.set("Authorization", `Bearer ${token}`);
    }
    return headers;
  }

  protected get<T>(
    path: string,
    params?: Record<string, string>,
  ): Observable<T> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
          httpParams = httpParams.set(k, v);
        }
      });
    }
    return this.http
      .get<T>(`${this.apiUrl}${path}`, {
        headers: this.getHeaders(),
        params: httpParams,
      })
      .pipe(catchError(this.handleError));
  }

  protected post<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .post<T>(`${this.apiUrl}${path}`, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  protected patch<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .patch<T>(`${this.apiUrl}${path}`, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  protected delete<T>(path: string): Observable<T> {
    return this.http
      .delete<T>(`${this.apiUrl}${path}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  protected getBlob(
    path: string,
    params?: Record<string, string>,
  ): Observable<Blob> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
          httpParams = httpParams.set(k, v);
        }
      });
    }
    return this.http
      .get(`${this.apiUrl}${path}`, {
        headers: this.getHeaders(),
        params: httpParams,
        responseType: "blob",
      })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    console.error("API Error:", error);
    return throwError(() => error);
  }
}