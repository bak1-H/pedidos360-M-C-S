import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RabbitAdminComponent } from './rabbit-admin.component';
import { RabbitAdminService } from './rabbit-admin.service';

const colaCreada = { nombre: 'cola.a', durable: true, deadLetterExchange: null, deadLetterRoutingKey: null };

function crear(servicio: Partial<RabbitAdminService>) {
  TestBed.configureTestingModule({
    imports: [RabbitAdminComponent],
    providers: [
      { provide: RabbitAdminService, useValue: servicio },
    ],
  });

  const fixture = TestBed.createComponent(RabbitAdminComponent);
  fixture.detectChanges();
  return fixture;
}

function escribir(html: HTMLElement, selector: string, valor: string): void {
  const campo = html.querySelector(selector) as HTMLInputElement;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
}

async function enviar(fixture: ReturnType<typeof crear>, selector: string): Promise<HTMLElement> {
  const html = fixture.nativeElement as HTMLElement;
  (html.querySelector(selector) as HTMLFormElement).dispatchEvent(new Event('submit'));
  await fixture.whenStable();
  fixture.detectChanges();
  return html;
}

describe('RabbitAdminComponent', () => {
  it('does not call the service when the queue name is invalid', async () => {
    const crearCola = vi.fn(() => of(colaCreada));
    const fixture = crear({ crearCola });
    const html = fixture.nativeElement as HTMLElement;

    escribir(html, '#cola-crear-nombre', 'nombre con espacios');
    await enviar(fixture, '#form-crear-cola');

    expect(crearCola).not.toHaveBeenCalled();
    expect(html.textContent).toContain('Solo admite letras');
  });

  it('creates a queue with the form values', async () => {
    const crearCola = vi.fn(() => of(colaCreada));
    const fixture = crear({ crearCola });
    const html = fixture.nativeElement as HTMLElement;

    escribir(html, '#cola-crear-nombre', 'cola.a');
    await enviar(fixture, '#form-crear-cola');

    expect(crearCola).toHaveBeenCalledWith({
      nombre: 'cola.a',
      durable: true,
      deadLetterExchange: undefined,
      deadLetterRoutingKey: undefined,
    });
    expect(html.textContent).toContain('Cola cola.a creada.');
  });

  it('shows the queue lookup result', async () => {
    const obtenerCola = vi.fn(() => of({ nombre: 'cola.a', mensajes: 3, consumidores: 1 }));
    const fixture = crear({ obtenerCola });
    const html = fixture.nativeElement as HTMLElement;

    escribir(html, '#cola-buscar-nombre', 'cola.a');
    await enviar(fixture, '#form-buscar-cola');

    expect(obtenerCola).toHaveBeenCalledWith('cola.a');
    expect(html.textContent).toContain('3 mensajes');
    expect(html.textContent).toContain('1 consumidor');
  });

  it('surfaces the server message when deleting a protected exchange', async () => {
    const eliminarExchange = vi.fn(() =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { status: 409, codigo: 'RABBIT_ADMIN_RECHAZO', mensaje: 'El exchange está protegido' },
          }),
      ),
    );
    const fixture = crear({ eliminarExchange });
    const html = fixture.nativeElement as HTMLElement;

    escribir(html, '#exchange-eliminar-nombre', 'pedidos.exchange');
    await enviar(fixture, '#form-eliminar-exchange');

    expect(eliminarExchange).toHaveBeenCalledWith('pedidos.exchange');
    expect(html.querySelector('[role="alert"]')?.textContent).toContain('El exchange está protegido');
  });

  it('deletes a binding with the form values', async () => {
    const eliminarBinding = vi.fn(() => of(undefined));
    const fixture = crear({ eliminarBinding });
    const html = fixture.nativeElement as HTMLElement;

    escribir(html, '#binding-exchange', 'ex.a');
    escribir(html, '#binding-destino', 'cola.a');
    escribir(html, '#binding-routing-key', 'k');
    (html.querySelector('#binding-eliminar') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(eliminarBinding).toHaveBeenCalledWith({
      exchange: 'ex.a',
      destino: 'cola.a',
      tipoDestino: 'queue',
      routingKey: 'k',
    });
  });
});
