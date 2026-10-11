import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BffService, EnvioResponseDto, PedidoResponseDto, UsuarioDto } from '../../core/services/bff.service';
import { AdminComponent } from './admin.component';

const authAdmin = {
  isAuthenticated: () => true,
  hasAnyRole: () => true,
};

const usuarios: UsuarioDto[] = [
  { id: 'r1', azureAdObjectId: 'a', nombre: 'Rita Repartidora', email: 'r@x.com', rol: 'REPARTIDOR', fechaCreacion: '' },
  { id: 'c1', azureAdObjectId: 'b', nombre: 'Carlos Cliente', email: 'c@x.com', rol: 'CLIENTE', fechaCreacion: '' },
];

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

const rechazo = (status: number, codigo: string) =>
  new HttpErrorResponse({ status, error: { status, codigo, mensaje: 'rechazo', ruta: '/bff/envios' } });

function crear(bff: Partial<BffService>) {
  TestBed.configureTestingModule({
    imports: [AdminComponent],
    providers: [
      { provide: AuthService, useValue: authAdmin },
      { provide: BffService, useValue: bff },
    ],
  });

  return TestBed.createComponent(AdminComponent);
}

async function estabilizar(fixture: ReturnType<typeof crear>): Promise<HTMLElement> {
  await fixture.whenStable();
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

function valoresDeSelect(raiz: HTMLElement, indice: number): string[] {
  return Array.from(raiz.querySelectorAll('select')[indice].querySelectorAll('option'))
    .map((opcion) => opcion.value)
    .filter(Boolean);
}

describe('AdminComponent', () => {
  const pedidos = [
    pedido('p1'),
    pedido('p2'),
    pedido('p3'),
    pedido('p4', 'CANCELADO'),
    pedido('p5'),
    pedido('p6', 'DESPACHADO'),
  ];
  let listado: EnvioResponseDto[];
  let obtenerEnvios: ReturnType<typeof vi.fn>;
  let obtenerPedido: ReturnType<typeof vi.fn>;

  const bffBase = (): Partial<BffService> => ({
    obtenerUsuarios: () => of(usuarios),
    obtenerPedidos: () => of(pedidos),
    obtenerEnvios: obtenerEnvios as never,
    obtenerPedido: obtenerPedido as never,
  });

  beforeEach(() => {
    listado = [envio('p2', 'PENDIENTE'), envio('p3', 'ENTREGADO'), envio('p5', 'FALLIDO'), envio('p6', 'EN_TRANSITO')];
    obtenerEnvios = vi.fn(() => of(listado));
    obtenerPedido = vi.fn();
  });

  it('does not write results once the component is destroyed', async () => {
    const usuariosPendientes = new Subject<UsuarioDto[]>();
    const fixture = crear({ ...bffBase(), obtenerUsuarios: () => usuariosPendientes });
    fixture.detectChanges();

    fixture.destroy();
    usuariosPendientes.next(usuarios);
    usuariosPendientes.complete();
    await new Promise((resolver) => setTimeout(resolver));

    const componente = fixture.componentInstance as unknown as { usuarios: () => UsuarioDto[]; pedidos: () => unknown[] };
    expect(componente.usuarios()).toEqual([]);
    expect(componente.pedidos()).toEqual([]);
  });

  it('builds the envio map from a single call to the envios listing', async () => {
    await estabilizar(crear(bffBase()));

    expect(obtenerEnvios).toHaveBeenCalledTimes(1);
    expect(obtenerPedido).not.toHaveBeenCalled();
  });

  it('offers only CREADO pedidos without any envio, excluding FALLIDO ones', async () => {
    const raiz = await estabilizar(crear(bffBase()));

    expect(valoresDeSelect(raiz, 0)).toEqual(['p1']);
  });

  it('lists only repartidores in the selector', async () => {
    const raiz = await estabilizar(crear(bffBase()));

    expect(valoresDeSelect(raiz, 1)).toEqual(['r1']);
  });

  it('shows a FALLIDO envio as Fallido even though the pedido is still CREADO and counts it', async () => {
    const fixture = crear(bffBase());
    const raiz = await estabilizar(fixture);
    const fila = Array.from(raiz.querySelectorAll('tbody tr')).find((tr) => tr.textContent?.includes('#P5'));

    expect(fila?.textContent).toContain('Fallido');
    expect(fila?.querySelector('.badge-error')).not.toBeNull();
    expect(fixture.componentInstance['fallidos']()).toBe(1);
  });

  it('prefers the envio state when it disagrees with the pedido state', async () => {
    const raiz = await estabilizar(crear(bffBase()));
    const filas = Array.from(raiz.querySelectorAll('tbody tr')).map((tr) => tr.textContent ?? '');

    expect(filas.find((texto) => texto.includes('#P6'))).toContain('En tránsito');
    expect(filas.find((texto) => texto.includes('#P2'))).toContain('Asignado');
  });

  it('removes the assigned pedido from the options after a successful assignment', async () => {
    const crearEnvio = vi.fn(() => {
      listado = [...listado, envio('p1', 'PENDIENTE')];
      return of(envio('p1', 'PENDIENTE'));
    });
    const fixture = crear({ ...bffBase(), crearEnvio });
    const raiz = await estabilizar(fixture);

    fixture.componentInstance['envioForm'].patchValue({ pedidoId: 'p1', repartidorId: 'r1' });
    await fixture.componentInstance.crearEnvio();
    fixture.detectChanges();

    expect(crearEnvio).toHaveBeenCalledTimes(1);
    expect(valoresDeSelect(raiz, 0)).toEqual([]);
    expect(raiz.textContent).toContain('asignado correctamente');
  });

  it('explains ENVIO_DUPLICADO by codigo, reloads and keeps the message', async () => {
    const crearEnvio = vi.fn(() => {
      listado = [...listado, envio('p1', 'PENDIENTE')];
      return throwError(() => rechazo(409, 'ENVIO_DUPLICADO'));
    });
    const fixture = crear({ ...bffBase(), crearEnvio });
    const raiz = await estabilizar(fixture);

    fixture.componentInstance['envioForm'].patchValue({ pedidoId: 'p1', repartidorId: 'r1' });
    await fixture.componentInstance.crearEnvio();
    fixture.detectChanges();

    expect(raiz.textContent).toContain('Este pedido ya tiene un envío asignado');
    expect(obtenerEnvios).toHaveBeenCalledTimes(2);
    expect(valoresDeSelect(raiz, 0)).toEqual([]);
    expect(fixture.componentInstance['saving']()).toBe(false);
  });

  it('explains PEDIDO_NO_ASIGNABLE by codigo and reloads', async () => {
    const crearEnvio = vi.fn(() => throwError(() => rechazo(409, 'PEDIDO_NO_ASIGNABLE')));
    const fixture = crear({ ...bffBase(), crearEnvio });
    const raiz = await estabilizar(fixture);

    fixture.componentInstance['envioForm'].patchValue({ pedidoId: 'p1', repartidorId: 'r1' });
    await fixture.componentInstance.crearEnvio();
    fixture.detectChanges();

    expect(raiz.textContent).toContain('El pedido ya no está disponible para asignar');
    expect(obtenerEnvios).toHaveBeenCalledTimes(2);
  });

  it('shows a generic retry message for 502 and 504 without a business codigo', async () => {
    for (const status of [502, 504]) {
      TestBed.resetTestingModule();
      const crearEnvio = vi.fn(() => throwError(() => rechazo(status, 'ERROR_MICROSERVICIO')));
      const fixture = crear({ ...bffBase(), crearEnvio });
      const raiz = await estabilizar(fixture);

      fixture.componentInstance['envioForm'].patchValue({ pedidoId: 'p1', repartidorId: 'r1' });
      await fixture.componentInstance.crearEnvio();
      fixture.detectChanges();

      expect(raiz.textContent).toContain('No pudimos asignar el envío. Intenta nuevamente.');
    }
  });

  it('keeps the specific message when the follow-up reload fails', async () => {
    const crearEnvio = vi.fn(() => throwError(() => rechazo(409, 'ENVIO_DUPLICADO')));
    const fixture = crear({ ...bffBase(), crearEnvio });
    const raiz = await estabilizar(fixture);
    obtenerEnvios.mockImplementation(() => throwError(() => new HttpErrorResponse({ status: 502 })));

    fixture.componentInstance['envioForm'].patchValue({ pedidoId: 'p1', repartidorId: 'r1' });
    await fixture.componentInstance.crearEnvio();
    fixture.detectChanges();

    expect(raiz.textContent).toContain('Este pedido ya tiene un envío asignado');
    expect(raiz.textContent).toContain('No pudimos cargar la información de la operación');
  });

  it('runs a queued reload right after the one in flight instead of dropping it', async () => {
    const fixture = crear(bffBase());
    await estabilizar(fixture);
    obtenerEnvios.mockClear();

    const primera = fixture.componentInstance.cargarDatos();
    const segunda = fixture.componentInstance.cargarDatos();
    const tercera = fixture.componentInstance.cargarDatos();
    await Promise.all([primera, segunda, tercera]);

    expect(obtenerEnvios).toHaveBeenCalledTimes(2);
  });

  it('ignores a submit while another one is in flight', async () => {
    const crearEnvio = vi.fn(() => of(envio('p1', 'PENDIENTE')));
    const fixture = crear({ ...bffBase(), crearEnvio });
    await estabilizar(fixture);
    fixture.componentInstance['saving'].set(true);
    fixture.componentInstance['envioForm'].patchValue({ pedidoId: 'p1', repartidorId: 'r1' });

    await fixture.componentInstance.crearEnvio();

    expect(crearEnvio).not.toHaveBeenCalled();
  });

  it('filters finished pedidos', async () => {
    const fixture = crear(bffBase());
    const raiz = await estabilizar(fixture);

    fixture.componentInstance.seleccionarFiltro('finalizado');
    fixture.detectChanges();

    const texto = Array.from(raiz.querySelectorAll('tbody tr'))
      .map((fila) => fila.textContent)
      .join(' ');
    expect(texto).toContain('Entregado');
    expect(texto).toContain('Cancelado');
    expect(texto).toContain('Fallido');
    expect(texto).not.toContain('Creado');
  });
});
