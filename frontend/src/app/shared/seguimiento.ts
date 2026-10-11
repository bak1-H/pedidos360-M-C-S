import { firstValueFrom, timeout } from 'rxjs';
import {
  BffService,
  CambioEstadoEnvioDto,
  EnvioResponseDto,
  PedidoResponseDto,
} from '../core/services/bff.service';

export type EstadoEnvio = CambioEstadoEnvioDto['estadoEnvio'];
export type GrupoEstado = 'sin-asignar' | 'en-curso' | 'finalizado';
export type EnviosPorPedido = Record<string, EnvioResponseDto | null>;

export interface EnviosCargados {
  envios: EnviosPorPedido;
  sinVerificar: string[];
}

export const TAMANO_LOTE = 6;
export const TIMEOUT_CONSULTA_MS = 10000;

const TRANSICIONES: Record<string, EstadoEnvio[]> = {
  PENDIENTE: ['EN_TRANSITO', 'FALLIDO'],
  EN_TRANSITO: ['ENTREGADO', 'FALLIDO'],
  ENTREGADO: [],
  FALLIDO: [],
};

const ESTADO_DE_ENVIO: Record<string, string> = {
  PENDIENTE: 'ASIGNADO',
  EN_TRANSITO: 'EN_TRANSITO',
  ENTREGADO: 'ENTREGADO',
  FALLIDO: 'FALLIDO',
};

const GRUPOS: Record<string, GrupoEstado> = {
  CREADO: 'sin-asignar',
  ASIGNADO: 'en-curso',
  DESPACHADO: 'en-curso',
  EN_TRANSITO: 'en-curso',
  ENTREGADO: 'finalizado',
  FALLIDO: 'finalizado',
  CANCELADO: 'finalizado',
};

export function transicionesPermitidas(estado: string): EstadoEnvio[] {
  return TRANSICIONES[estado] ?? [];
}

export function envioFinalizado(estado: string): boolean {
  return transicionesPermitidas(estado).length === 0;
}

export function estadoEfectivo(pedido: PedidoResponseDto, envio: EnvioResponseDto | null | undefined): string {
  if (pedido.estado === 'CANCELADO' || !envio) {
    return pedido.estado;
  }

  return ESTADO_DE_ENVIO[envio.estadoEnvio] ?? pedido.estado;
}

export function esAsignable(pedido: PedidoResponseDto, envios: EnviosPorPedido): boolean {
  return pedido.estado === 'CREADO' && envios[pedido.id] === null;
}

export function grupoEstado(estado: string): GrupoEstado {
  return GRUPOS[estado] ?? 'en-curso';
}

export function claseEstado(estado: string): string {
  switch (estado) {
    case 'ENTREGADO':
      return 'badge-success';
    case 'FALLIDO':
    case 'CANCELADO':
      return 'badge-error';
    case 'CREADO':
      return '';
    default:
      return 'badge-warning';
  }
}

export function enviosDesdeListado(pedidos: PedidoResponseDto[], envios: EnvioResponseDto[]): EnviosPorPedido {
  const porPedido = new Map(envios.map((envio) => [envio.pedidoId, envio]));

  return Object.fromEntries(pedidos.map((pedido) => [pedido.id, porPedido.get(pedido.id) ?? null]));
}

export async function cargarEnviosPorPedido(
  bff: BffService,
  pedidos: PedidoResponseDto[],
  cancelado: () => boolean = () => false,
): Promise<EnviosCargados> {
  const envios: EnviosPorPedido = {};
  const sinVerificar: string[] = [];
  const consultables = pedidos.filter((pedido) => pedido.estado !== 'CANCELADO');

  pedidos
    .filter((pedido) => pedido.estado === 'CANCELADO')
    .forEach((pedido) => {
      envios[pedido.id] = null;
    });

  for (let inicio = 0; inicio < consultables.length; inicio += TAMANO_LOTE) {
    if (cancelado()) {
      break;
    }

    const lote = consultables.slice(inicio, inicio + TAMANO_LOTE);
    const resultados = await Promise.allSettled(
      lote.map((pedido) => firstValueFrom(bff.obtenerPedido(pedido.id).pipe(timeout(TIMEOUT_CONSULTA_MS)))),
    );

    resultados.forEach((resultado, indice) => {
      const id = lote[indice].id;

      if (resultado.status === 'fulfilled') {
        envios[id] = resultado.value.envio;
      } else {
        sinVerificar.push(id);
      }
    });
  }

  return { envios, sinVerificar };
}
