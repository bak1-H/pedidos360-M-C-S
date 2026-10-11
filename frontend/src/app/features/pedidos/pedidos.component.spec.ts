import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BffService, EnvioResponseDto, PedidoResponseDto } from '../../core/services/bff.service';
import { PedidosComponent } from './pedidos.component';

const pedido = (id: string, estado = 'CREADO'): PedidoResponseDto => ({
  id,
  estado,
  direccionOrigen: 'Origen',
  direccionDestino: 'Destino',
});

const envio = (pedidoId: string, estadoEnvio: string): EnvioResponseDto => ({
  id: `e-${pedidoId}`,
  pedidoId,
  repartidorId: 'r1',
  estadoEnvio,
  fechaAsignacion: null,
  fechaEntregaEstimada: null,
});

function crear(rol: 'ADMIN' | 'CLIENTE', bff: Partial<BffService>) {
  TestBed.configureTestingModule({
    imports: [PedidosComponent],
    providers: [
      {
        provide: AuthService,
        useValue: { isAuthenticated: () => true, hasAnyRole: (roles: string[]) => roles.includes(rol) },
      },
      { provide: BffService, useValue: bff },
    ],
  });

  return TestBed.createComponent(PedidosComponent);
}

async function estabilizar(fixture: ReturnType<typeof crear>): Promise<HTMLElement> {
  await fixture.whenStable();
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('PedidosComponent cleanup', () => {
  it('starts no further batches and writes nothing once destroyed', async () => {
    const muchos = Array.from({ length: 13 }, (_, indice) => pedido(`m${indice}`));
    const respuestas: Subject<{ pedido: never; envio: null }>[] = [];
    const obtenerPedido = vi.fn(() => {
      const respuesta = new Subject<{ pedido: never; envio: null }>();
      respuestas.push(respuesta);
      return respuesta;
    });
    const fixture = crear('CLIENTE', { obtenerMisPedidos: () => of(muchos), obtenerPedido: obtenerPedido as never });
    fixture.detectChanges();
    await vi.waitFor(() => expect(obtenerPedido).toHaveBeenCalledTimes(6));

    fixture.destroy();
    respuestas.forEach((respuesta) => {
      respuesta.next({ pedido: pedido('x') as never, envio: null });
      respuesta.complete();
    });
    await new Promise((resolver) => setTimeout(resolver));

    expect(obtenerPedido).toHaveBeenCalledTimes(6);
    const componente = fixture.componentInstance as unknown as { pedidos: () => unknown[] };
    expect(componente.pedidos()).toEqual([]);
  });
});

describe('PedidosComponent', () => {
  const lista = [pedido('p1'), pedido('p2'), pedido('p3'), pedido('p4'), pedido('p5', 'DESPACHADO')];
  const detalle: Record<string, EnvioResponseDto | null> = {
    p1: null,
    p2: envio('p2', 'EN_TRANSITO'),
    p3: envio('p3', 'ENTREGADO'),
    p4: envio('p4', 'FALLIDO'),
    p5: null,
  };

  const bff = (): Partial<BffService> => ({
    obtenerMisPedidos: () => of(lista),
    obtenerPedidos: () => of(lista),
    obtenerEnvios: () => of(Object.values(detalle).filter((valor): valor is EnvioResponseDto => valor !== null)),
    obtenerPedido: (id: string) => of({ pedido: pedido(id) as never, envio: detalle[id] }),
  });

  it('shows the state of the envio instead of the stale pedido state', async () => {
    const raiz = await estabilizar(crear('CLIENTE', bff()));
    const filas = Array.from(raiz.querySelectorAll('tbody tr')).map((fila) => fila.textContent);

    expect(filas[0]).toContain('Creado');
    expect(filas[1]).toContain('En tránsito');
    expect(filas[2]).toContain('Entregado');
    expect(filas[3]).toContain('Fallido');
    expect(filas[4]).toContain('Despachado');
  });

  it('counts en camino, entregados, fallidos and otros so they add up to the total', async () => {
    const fixture = crear('CLIENTE', bff());
    await estabilizar(fixture);
    const instancia = fixture.componentInstance;

    expect(instancia['enCamino']()).toBe(1);
    expect(instancia['entregados']()).toBe(1);
    expect(instancia['fallidos']()).toBe(1);
    expect(instancia['otros']()).toBe(2);
  });

  it('loads the envios of an admin with a single listing call and no per-pedido lookups', async () => {
    const obtenerEnvios = vi.fn(bff().obtenerEnvios as never);
    const obtenerPedido = vi.fn();
    const raiz = await estabilizar(crear('ADMIN', { ...bff(), obtenerEnvios, obtenerPedido }));

    expect(obtenerEnvios).toHaveBeenCalledTimes(1);
    expect(obtenerPedido).not.toHaveBeenCalled();
    expect(Array.from(raiz.querySelectorAll('tbody tr'))[1].textContent).toContain('En tránsito');
  });

  it('falls back to the registered pedido state and warns when a lookup could not be verified', async () => {
    const obtenerPedido = (id: string) =>
      id === 'p2' ? throwError(() => new Error('fallo')) : of({ pedido: pedido(id) as never, envio: detalle[id] });
    const raiz = await estabilizar(crear('CLIENTE', { ...bff(), obtenerPedido: obtenerPedido as never }));
    const filas = Array.from(raiz.querySelectorAll('tbody tr')).map((fila) => fila.textContent);

    expect(filas[1]).toContain('Creado');
    expect(raiz.textContent).toContain('No pudimos verificar el envío de algunos pedidos');
  });

  it('runs a queued reload right after the one in flight', async () => {
    const obtenerMisPedidos = vi.fn(() => of(lista));
    const fixture = crear('CLIENTE', { ...bff(), obtenerMisPedidos });
    await estabilizar(fixture);
    obtenerMisPedidos.mockClear();

    await Promise.all([
      fixture.componentInstance.cargarPedidos(),
      fixture.componentInstance.cargarPedidos(),
      fixture.componentInstance.cargarPedidos(),
    ]);

    expect(obtenerMisPedidos).toHaveBeenCalledTimes(2);
  });

  it('uses the admin empty message for admins', async () => {
    const raiz = await estabilizar(
      crear('ADMIN', { ...bff(), obtenerPedidos: () => of([]), obtenerEnvios: () => of([]) }),
    );

    expect(raiz.textContent).toContain('No hay pedidos registrados');
    expect(raiz.textContent).not.toContain('Todavía no tienes pedidos');
  });

  it('ignores a second detail request while one is loading', async () => {
    const obtenerPedido = vi.fn((id: string) => of({ pedido: pedido(id) as never, envio: detalle[id] }));
    const fixture = crear('CLIENTE', { ...bff(), obtenerPedido });
    await estabilizar(fixture);
    obtenerPedido.mockClear();
    fixture.componentInstance['cargandoDetalle'].set(true);

    await fixture.componentInstance.verDetalle('p1');

    expect(obtenerPedido).not.toHaveBeenCalled();
  });
});
