import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_ENVIRONMENT } from '../config/app-environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = APP_ENVIRONMENT.apiBaseUrl;

  get<T>(path: string, params?: Record<string, string | number | boolean | undefined | null>): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`, { params: this.buildParams(params) });
  }

  getBlob(path: string, params?: Record<string, string | number | boolean | undefined | null>): Observable<Blob> {
    return this.http.get(`${this.baseUrl}${path}`, {
      params: this.buildParams(params),
      responseType: 'blob',
    });
  }

  post<T>(path: string, body: unknown = null, idempotent = false): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body, {
      headers: idempotent ? this.idempotencyHeaders() : undefined,
    });
  }

  put<T>(path: string, body: unknown, idempotent = false): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body, {
      headers: idempotent ? this.idempotencyHeaders() : undefined,
    });
  }

  newIdempotencyKey(): string {
    return crypto.randomUUID();
  }

  private idempotencyHeaders(): HttpHeaders {
    return new HttpHeaders({ 'Idempotency-Key': this.newIdempotencyKey() });
  }

  private buildParams(
    raw?: Record<string, string | number | boolean | undefined | null>,
  ): HttpParams | undefined {
    if (!raw) {
      return undefined;
    }
    let params = new HttpParams();
    for (const [key, value] of Object.entries(raw)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
