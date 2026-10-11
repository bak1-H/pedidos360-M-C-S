import { NEVER, Subject, defer, finalize, of, throwError } from 'rxjs';
import { BffService, EnvioResponseDto, PedidoResponseDto } from '../core/services/bff.service';
import {
  TAMANO_LOTE,
  TIMEOUT_CONSULTA_MS,
  cargarEnviosPorPedido,
  claseEstado,
  enviosDesdeListado,
  envioFinalizado,
  esAsignable,
  estadoEfectivo,
  grupoEstado,
  transicionesPermitidas,
} from './seguimiento';

const pedido = (id: string, estado = 'CREADO'): PedidoResponseDto => ({
  id,
  estado,
  direccionOrigen: 'A',
  direccionDestino: 'B',
});

const envio = (estadoEnvio: string, pedidoId = 'p1'): EnvioResponseDto => ({
  id: `e-${pedidoId}`,
  pedidoId,
  repartidorId: 'r1',
  estadoEnvio,
  fechaAsignacion: null,
  fechaEntregaEstimada: null,
});

describe('seguimiento', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('only offers forward transitions and none for finished envios', () => {
    expect(transicionesPermitidas('PENDIENTE')).toEqual(['EN_TRANSITO', 'FALLIDO']);
    expect(transicionesPermitidas('EN_TRANSITO')).toEqual(['ENTREGADO', 'FALLIDO']);
    expect(transicionesPermitidas('ENTREGADO')).toEqual([]);
    expect(envioFinalizado('FALLIDO')).toBe(true);
    expect(envioFinalizado('EN_TRANSITO')).toBe(false);
  });

  it('derives the effective state from the envio when it disagrees with the pedido', () => {
    expect(estadoEfectivo(pedido('p1'), null)).toBe('CREADO');
    expect(estadoEfectivo(pedido('p1'), envio('PENDIENTE'))).toBe('ASIGNADO');
    expect(estadoEfectivo(pedido('p1', 'DESPACHADO'), envio('PENDIENTE'))).toBe('ASIGNADO');
    expect(estadoEfectivo(pedido('p1', 'DESPACHADO'), envio('EN_TRANSITO'))).toBe('EN_TRANSITO');
    expect(estadoEfectivo(pedido('p1', 'DESPACHADO'), null)).toBe('DESPACHADO');
    expect(estadoEfectivo(pedido('p1'), envio('ENTREGADO'))).toBe('ENTREGADO');
    expect(estadoEfectivo(pedido('p1', 'ENTREGADO'), envio('ENTREGADO'))).toBe('ENTREGADO');
    expect(estadoEfectivo(pedido('p1', 'CANCELADO'), envio('PENDIENTE'))).toBe('CANCELADO');
  });

  it('shows FALLIDO envios as Fallido whatever the pedido state is', () => {
    expect(estadoEfectivo(pedido('p1', 'CREADO'), envio('FALLIDO'))).toBe('FALLIDO');
    expect(estadoEfectivo(pedido('p1', 'DESPACHADO'), envio('FALLIDO'))).toBe('FALLIDO');
  });

  it('only treats CREADO pedidos with no envio at all as assignable', () => {
    const envios = {
      libre: null,
      pendiente: envio('PENDIENTE', 'pendiente'),
      fallido: envio('FALLIDO', 'fallido'),
      entregado: envio('ENTREGADO', 'entregado'),
    };

    expect(esAsignable(pedido('libre'), envios)).toBe(true);
    expect(esAsignable(pedido('pendiente'), envios)).toBe(false);
    expect(esAsignable(pedido('fallido'), envios)).toBe(false);
    expect(esAsignable(pedido('entregado'), envios)).toBe(false);
    expect(esAsignable(pedido('libre', 'DESPACHADO'), envios)).toBe(false);
    expect(esAsignable(pedido('libre', 'CANCELADO'), envios)).toBe(false);
    expect(esAsignable(pedido('desconocido'), envios)).toBe(false);
  });

  it('groups states and maps badge classes', () => {
    expect(grupoEstado('CREADO')).toBe('sin-asignar');
    expect(grupoEstado('ASIGNADO')).toBe('en-curso');
    expect(grupoEstado('FALLIDO')).toBe('finalizado');
    expect(claseEstado('ENTREGADO')).toBe('badge-success');
    expect(claseEstado('EN_TRANSITO')).toBe('badge-warning');
    expect(claseEstado('FALLIDO')).toBe('badge-error');
    expect(claseEstado('CANCELADO')).toBe('badge-error');
  });

  it('maps a bare envios listing to a pedidoId lookup with null for pedidos without envio', () => {
    const resultado = enviosDesdeListado(
      [pedido('p1'), pedido('p2'), pedido('p3')],
      [envio('FALLIDO', 'p2'), envio('PENDIENTE', 'otro')],
    );

    expect(resultado['p1']).toBeNull();
    expect(resultado['p2']?.estadoEnvio).toBe('FALLIDO');
    expect(resultado['p3']).toBeNull();
    expect(resultado['otro']).toBeUndefined();
  });

  it('loads the envio of each pedido and reports the ones it could not verify', async () => {
    const bff = {
      obtenerPedido: (id: string) =>
        id === 'p1'
          ? of({ pedido: {}, envio: envio('PENDIENTE') })
          : id === 'p2'
            ? of({ pedido: {}, envio: null })
            : throwError(() => new Error('fallo')),
    } as unknown as BffService;

    const resultado = await cargarEnviosPorPedido(bff, [
      pedido('p1'),
      pedido('p2'),
      pedido('p3'),
      pedido('p4', 'CANCELADO'),
    ]);

    expect(resultado.envios['p1']?.estadoEnvio).toBe('PENDIENTE');
    expect(resultado.envios['p2']).toBeNull();
    expect(resultado.envios['p4']).toBeNull();
    expect(resultado.envios['p3']).toBeUndefined();
    expect(resultado.sinVerificar).toEqual(['p3']);
  });

  it('never has more than one batch of lookups in flight', async () => {
    const total = TAMANO_LOTE * 2 + 1;
    const pedidos = Array.from({ length: total }, (_, i) => pedido(`p${i}`));
    const abiertos = new Map<string, Subject<{ envio: null }>>();
    let enVuelo = 0;
    let maximo = 0;
    const bff = {
      obtenerPedido: (id: string) =>
        defer(() => {
          const sujeto = new Subject<{ envio: null }>();
          abiertos.set(id, sujeto);
          enVuelo += 1;
          maximo = Math.max(maximo, enVuelo);
          return sujeto;
        }).pipe(
          finalize(() => {
            enVuelo -= 1;
          }),
        ),
    } as unknown as BffService;
    const liberar = () => {
      for (const [id, sujeto] of [...abiertos]) {
        abiertos.delete(id);
        sujeto.next({ envio: null });
      }
    };

    const promesa = cargarEnviosPorPedido(bff, pedidos);
    await vi.waitFor(() => expect(abiertos.size).toBe(TAMANO_LOTE));
    liberar();
    await vi.waitFor(() => expect(abiertos.size).toBe(TAMANO_LOTE));
    liberar();
    await vi.waitFor(() => expect(abiertos.size).toBe(1));
    liberar();
    const resultado = await promesa;

    expect(maximo).toBe(TAMANO_LOTE);
    expect(Object.keys(resultado.envios)).toHaveLength(total);
    expect(resultado.sinVerificar).toEqual([]);
  });

  it('gives up on a hung lookup after the timeout so the load can finish', async () => {
    vi.useFakeTimers();
    const bff = {
      obtenerPedido: (id: string) => (id === 'p1' ? NEVER : of({ pedido: {}, envio: null })),
    } as unknown as BffService;

    const promesa = cargarEnviosPorPedido(bff, [pedido('p1'), pedido('p2')]);
    await vi.advanceTimersByTimeAsync(TIMEOUT_CONSULTA_MS);
    const resultado = await promesa;

    expect(resultado.sinVerificar).toEqual(['p1']);
    expect(resultado.envios['p2']).toBeNull();
  });
});
