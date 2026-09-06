import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { appSettings } from '../config/app-settings';

export interface PerfilResponse {
  id: string;
  azureAdObjectId: string;
  nombre: string;
  email: string;
  roles: string[];
  recienCreado: boolean;
}

export interface UsuarioDto {
  id: string;
  azureAdObjectId: string;
  nombre: string;
  email: string;
  rol: string;
  fechaCreacion: string;
}

export interface DetallePedidoDto {
  descripcion: string;
  cantidad: number;
  valorDeclarado: number;
}

export interface PaqueteDto {
  pesoKg: number;
  alturaCm: number;
  anchoCm: number;
  largoCm: number;
  tipo: 'DOCUMENTO' | 'CAJA' | 'FRAGIL';
}

export interface CrearPedidoRequest {
  direccionOrigen: string;
  direccionDestino: string;
  detalles: DetallePedidoDto[];
  paquete: PaqueteDto;
}

export interface PedidoResponseDto {
  id: string;
  clienteId?: string;
  estado: string;
  direccionOrigen: string;
  direccionDestino: string;
  fechaCreacion?: string;
}

export interface PedidoConTrackingResponse {
  pedido: {
    id: string;
    clienteId: string;
    estado: string;
    direccionOrigen: string;
    direccionDestino: string;
    fechaCreacion: string;
    detalles: DetallePedidoDto[];
    paquete: PaqueteDto;
  };
  envio: EnvioResponseDto | null;
}

export interface EnvioResponseDto {
  id: string;
  pedidoId: string;
  repartidorId: string | null;
  estadoEnvio: string;
  fechaAsignacion: string | null;
  fechaEntregaEstimada: string | null;
}

export interface EnvioRequestDto {
  pedidoId: string;
  repartidorId: string;
  fechaEntregaEstimada?: string | null;
}

export interface CambioEstadoEnvioDto {
  estadoEnvio: 'PENDIENTE' | 'EN_TRANSITO' | 'ENTREGADO' | 'FALLIDO';
  descripcionEvento: string;
}

@Injectable({
  providedIn: 'root',
})
export class BffService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = appSettings.bffApiUrl.replace(/\/$/, '');

  me(): Observable<PerfilResponse> {
    return this.http.get<PerfilResponse>(`${this.baseUrl}/me`);
  }

  obtenerUsuarios(): Observable<UsuarioDto[]> {
    return this.http.get<UsuarioDto[]>(`${this.baseUrl}/usuarios`);
  }

  crearPedido(request: CrearPedidoRequest): Observable<PedidoResponseDto> {
    return this.http.post<PedidoResponseDto>(`${this.baseUrl}/pedidos`, request);
  }

  obtenerMisPedidos(): Observable<PedidoResponseDto[]> {
    return this.http.get<PedidoResponseDto[]>(`${this.baseUrl}/pedidos/mios`);
  }

  obtenerPedidos(): Observable<PedidoResponseDto[]> {
    return this.http.get<PedidoResponseDto[]>(`${this.baseUrl}/pedidos`);
  }

  obtenerPedido(id: string): Observable<PedidoConTrackingResponse> {
    return this.http.get<PedidoConTrackingResponse>(`${this.baseUrl}/pedidos/${id}`);
  }

  crearEnvio(request: EnvioRequestDto): Observable<EnvioResponseDto> {
    return this.http.post<EnvioResponseDto>(`${this.baseUrl}/envios`, request);
  }

  obtenerMisEnvios(): Observable<EnvioResponseDto[]> {
    return this.http.get<EnvioResponseDto[]>(`${this.baseUrl}/envios/mios`);
  }

  cambiarEstadoEnvio(id: string, request: CambioEstadoEnvioDto): Observable<EnvioResponseDto> {
    return this.http.patch<EnvioResponseDto>(`${this.baseUrl}/envios/${id}/estado`, request);
  }
}