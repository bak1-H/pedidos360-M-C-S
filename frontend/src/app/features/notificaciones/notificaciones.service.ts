import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { appSettings } from '../../core/config/app-settings';
import {
  Cola,
  FiltroNotificaciones,
  PaginaNotificaciones,
  ResumenNotificaciones,
} from './notificaciones.models';

@Injectable({
  providedIn: 'root',
})
export class NotificacionesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${appSettings.bffApiUrl.replace(/\/$/, '')}/notificaciones`;

  listar(filtro: FiltroNotificaciones): Observable<PaginaNotificaciones> {
    let params = new HttpParams().set('pagina', filtro.pagina).set('tamano', filtro.tamano);

    if (filtro.tipo) {
      params = params.set('tipo', filtro.tipo);
    }

    if (filtro.estado) {
      params = params.set('estado', filtro.estado);
    }

    return this.http.get<PaginaNotificaciones>(this.baseUrl, { params });
  }

  resumen(): Observable<ResumenNotificaciones> {
    return this.http.get<ResumenNotificaciones>(`${this.baseUrl}/resumen`);
  }

  colas(): Observable<Cola[]> {
    return this.http.get<Cola[]>(`${this.baseUrl}/colas`);
  }
}
