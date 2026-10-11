export const TIPOS_NOTIFICACION = ['PEDIDO_CREADO'] as const;

export const ESTADOS_NOTIFICACION = ['PROCESADA', 'MUERTA'] as const;

export type TipoNotificacion = (typeof TIPOS_NOTIFICACION)[number];
export type EstadoNotificacion = (typeof ESTADOS_NOTIFICACION)[number];
export type CategoriaCola = 'NEGOCIO' | 'DLQ';

export interface Notificacion {
  id: string;
  tipo: TipoNotificacion;
  estado: EstadoNotificacion;
  routingKey: string;
  cola: string;
  destinatario: string;
  mensaje: string;
  referenciaId: string | null;
  fechaRecepcion: string;
}

export interface PaginaNotificaciones {
  contenido: Notificacion[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface ResumenNotificaciones {
  total: number;
  porTipo: Record<string, number>;
  porEstado: { PROCESADA: number; MUERTA: number };
}

export interface Cola {
  nombre: string;
  categoria: CategoriaCola;
  mensajes: number;
  consumidores: number;
}

export interface FiltroNotificaciones {
  tipo: TipoNotificacion | '';
  estado: EstadoNotificacion | '';
  pagina: number;
  tamano: number;
}

export interface FlujoCola {
  base: string;
  negocio: Cola | null;
  dlq: Cola | null;
}
