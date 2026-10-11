import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BffService, EnvioResponseDto } from '../../core/services/bff.service';
import { EnviosComponent } from './envios.component';

const authRepartidor = {
  isAuthenticated: () => true,
  hasAnyRole: () => true,
};

const envio = (id: string, estadoEnvio: string): EnvioResponseDto => ({
  id,
  pedidoId: `p-${id}`,
  repartidorId: 'r1',
  estadoEnvio,
  fechaAsignacion: null,
  fechaEntregaEstimada: null,
});

function crear(bff: Partial<BffService>) {
  TestBed.configureTestingModule({
    imports: [EnviosComponent],
    providers: [
      { provide: AuthService, useValue: authRepartidor },
      { provide: BffService, useValue: bff },
    ],
  });

  return TestBed.createComponent(EnviosComponent);
}

async function estabilizar(fixture: ReturnType<typeof crear>): Promise<HTMLElement> {
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

const lista = [envio('e-fin', 'ENTREGADO'), envio('e-pend', 'PENDIENTE'), envio('e-ruta', 'EN_TRANSITO')];

describe('EnviosComponent', () => {
  it('puts active envios first and hides the action on finished ones', async () => {
    const raiz = await estabilizar(crear({ obtenerMisEnvios: () => of(lista) }));
    const filas = Array.from(raiz.querySelectorAll('tbody tr'));

    expect(filas[2].textContent).toContain('Entregado');
    expect(filas[2].querySelector('button')).toBeNull();
    expect(filas[2].textContent).toContain('Entrega finalizada');
    expect(filas[0].querySelector('button')).not.toBeNull();
  });

  it('offers only valid next states', async () => {
    const fixture = crear({ obtenerMisEnvios: () => of(lista) });
    const raiz = await estabilizar(fixture);

    fixture.componentInstance.seleccionarEnvio(lista[1]);
    fixture.detectChanges();
    const opciones = Array.from(raiz.querySelectorAll('select option')).map((o) => (o as HTMLOptionElement).value);

    expect(opciones).toEqual(['EN_TRANSITO', 'FALLIDO']);
  });

  it('does not open the form for a finished envio', async () => {
    const fixture = crear({ obtenerMisEnvios: () => of(lista) });
    await estabilizar(fixture);

    fixture.componentInstance.seleccionarEnvio(lista[0]);

    expect(fixture.componentInstance['envioSeleccionado']()).toBeNull();
  });

  it('refreshes the list after saving a new state', async () => {
    const obtenerMisEnvios = vi.fn(() => of(lista));
    const cambiarEstadoEnvio = vi.fn(() => of(envio('e-pend', 'EN_TRANSITO')));
    const fixture = crear({ obtenerMisEnvios, cambiarEstadoEnvio });
    await estabilizar(fixture);

    fixture.componentInstance.seleccionarEnvio(lista[1]);
    fixture.componentInstance['cambioEstadoForm'].patchValue({ descripcionEvento: 'Salio a ruta' });
    await fixture.componentInstance.guardarEstado();

    expect(cambiarEstadoEnvio).toHaveBeenCalledWith('e-pend', {
      estadoEnvio: 'EN_TRANSITO',
      descripcionEvento: 'Salio a ruta',
    });
    expect(obtenerMisEnvios).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance['envioSeleccionado']()).toBeNull();
  });

  it('ignores a second save while one is in flight', async () => {
    const cambiarEstadoEnvio = vi.fn(() => of(envio('e-pend', 'EN_TRANSITO')));
    const fixture = crear({ obtenerMisEnvios: () => of(lista), cambiarEstadoEnvio });
    await estabilizar(fixture);

    fixture.componentInstance.seleccionarEnvio(lista[1]);
    fixture.componentInstance['cambioEstadoForm'].patchValue({ descripcionEvento: 'Salio a ruta' });
    fixture.componentInstance['saving'].set(true);
    await fixture.componentInstance.guardarEstado();

    expect(cambiarEstadoEnvio).not.toHaveBeenCalled();
  });

  const rechazo = (status: number, codigo: string) =>
    new HttpErrorResponse({ status, error: { status, codigo, mensaje: 'rechazo', ruta: '/bff/envios' } });

  async function guardarConFallo(fallo: HttpErrorResponse, listados = vi.fn(() => of(lista))) {
    const cambiarEstadoEnvio = vi.fn(() => throwError(() => fallo));
    const fixture = crear({ obtenerMisEnvios: listados, cambiarEstadoEnvio });
    const raiz = await estabilizar(fixture);

    fixture.componentInstance.seleccionarEnvio(lista[1]);
    fixture.componentInstance['cambioEstadoForm'].patchValue({ descripcionEvento: 'Salio a ruta' });
    await fixture.componentInstance.guardarEstado();
    fixture.detectChanges();

    return { raiz, listados, fixture };
  }

  it('explains a forbidden change by codigo and reloads the list', async () => {
    const { raiz, listados } = await guardarConFallo(rechazo(403, 'SIN_PERMISO'));

    expect(raiz.textContent).toContain('ya no está asignado a ti');
    expect(listados).toHaveBeenCalledTimes(2);
  });

  it('explains TRANSICION_INVALIDA and reloads the list', async () => {
    const { raiz, listados } = await guardarConFallo(rechazo(409, 'TRANSICION_INVALIDA'));

    expect(raiz.textContent).toContain('El envío ya cambió de estado; se actualizó la lista');
    expect(listados).toHaveBeenCalledTimes(2);
  });

  it('explains NO_ENCONTRADO', async () => {
    const { raiz } = await guardarConFallo(rechazo(404, 'NO_ENCONTRADO'));

    expect(raiz.textContent).toContain('No encontramos ese envío');
  });

  it('shows a generic retry message for 502 and 504', async () => {
    for (const status of [502, 504]) {
      TestBed.resetTestingModule();
      const { raiz } = await guardarConFallo(rechazo(status, 'ERROR_MICROSERVICIO'));

      expect(raiz.textContent).toContain('No pudimos actualizar el envío. Intenta nuevamente');
    }
  });

  it('keeps the specific message when the reload after a 409 fails', async () => {
    let llamadas = 0;
    const listados = vi.fn(() => {
      llamadas += 1;
      return llamadas === 1 ? of(lista) : throwError(() => new HttpErrorResponse({ status: 502 }));
    });
    const { raiz } = await guardarConFallo(rechazo(409, 'TRANSICION_INVALIDA'), listados);

    expect(raiz.textContent).toContain('El envío ya cambió de estado');
    expect(raiz.textContent).toContain('No pudimos cargar tus envíos asignados');
  });

  it('runs a queued reload right after the one in flight', async () => {
    const obtenerMisEnvios = vi.fn(() => of(lista));
    const fixture = crear({ obtenerMisEnvios });
    await estabilizar(fixture);
    obtenerMisEnvios.mockClear();

    await Promise.all([
      fixture.componentInstance.cargarEnvios(),
      fixture.componentInstance.cargarEnvios(),
      fixture.componentInstance.cargarEnvios(),
    ]);

    expect(obtenerMisEnvios).toHaveBeenCalledTimes(2);
  });

  it('counts pendientes, en tránsito, entregados and fallidos so they add up to the total', async () => {
    const todos = [...lista, envio('e-fail', 'FALLIDO')];
    const fixture = crear({ obtenerMisEnvios: () => of(todos) });
    const raiz = await estabilizar(fixture);
    const instancia = fixture.componentInstance;

    expect(instancia['pendientes']() + instancia['enTransito']() + instancia['entregados']() + instancia['fallidos']()).toBe(
      todos.length,
    );
    expect(raiz.textContent).toContain('Fallidos');
  });
});
