export const PATRON_NOMBRE_RABBIT = /^[A-Za-z0-9._-]{1,255}$/;

export const TIPOS_EXCHANGE = ['direct', 'topic', 'fanout', 'headers'] as const;
export type TipoExchange = (typeof TIPOS_EXCHANGE)[number];

export const TIPOS_DESTINO = ['queue', 'exchange'] as const;
export type TipoDestino = (typeof TIPOS_DESTINO)[number];

export interface CrearColaRequest {
  nombre: string;
  durable?: boolean;
  deadLetterExchange?: string;
  deadLetterRoutingKey?: string;
}

export interface CrearExchangeRequest {
  nombre: string;
  tipo: TipoExchange;
  durable?: boolean;
}

export interface BindingRequest {
  exchange: string;
  destino: string;
  tipoDestino?: TipoDestino;
  routingKey?: string;
}

export interface ColaResponse {
  nombre: string;
  durable: boolean;
  deadLetterExchange: string | null;
  deadLetterRoutingKey: string | null;
}

export interface ColaInfoResponse {
  nombre: string;
  mensajes: number;
  consumidores: number;
}

export interface ExchangeResponse {
  nombre: string;
  tipo: string;
  durable: boolean;
}
