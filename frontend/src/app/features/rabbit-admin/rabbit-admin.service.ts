import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { appSettings } from '../../core/config/app-settings';
import {
  BindingRequest,
  ColaInfoResponse,
  ColaResponse,
  CrearColaRequest,
  CrearExchangeRequest,
  ExchangeResponse,
} from './rabbit-admin.models';

@Injectable({
  providedIn: 'root',
})
export class RabbitAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${appSettings.bffApiUrl.replace(/\/$/, '')}/rabbit`;

  crearCola(request: CrearColaRequest): Observable<ColaResponse> {
    return this.http.post<ColaResponse>(`${this.baseUrl}/queues`, request);
  }

  obtenerCola(nombre: string): Observable<ColaInfoResponse> {
    return this.http.get<ColaInfoResponse>(`${this.baseUrl}/queues/${encodeURIComponent(nombre)}`);
  }

  eliminarCola(nombre: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/queues/${encodeURIComponent(nombre)}`);
  }

  crearExchange(request: CrearExchangeRequest): Observable<ExchangeResponse> {
    return this.http.post<ExchangeResponse>(`${this.baseUrl}/exchanges`, request);
  }

  eliminarExchange(nombre: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/exchanges/${encodeURIComponent(nombre)}`);
  }

  crearBinding(request: BindingRequest): Observable<unknown> {
    return this.http.post<unknown>(`${this.baseUrl}/bindings`, request);
  }

  eliminarBinding(request: BindingRequest): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/bindings`, { body: request });
  }
}
