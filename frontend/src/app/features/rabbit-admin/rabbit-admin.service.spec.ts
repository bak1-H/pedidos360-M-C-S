import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { appSettings } from '../../core/config/app-settings';
import { RabbitAdminService } from './rabbit-admin.service';

describe('RabbitAdminService', () => {
  const base = `${appSettings.bffApiUrl.replace(/\/$/, '')}/rabbit`;
  let servicio: RabbitAdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(RabbitAdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a queue', () => {
    const cuerpo = { nombre: 'cola.a', durable: true };
    servicio.crearCola(cuerpo).subscribe();

    const req = http.expectOne(`${base}/queues`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({});
  });

  it('looks up a queue by encoded name', () => {
    servicio.obtenerCola('cola a/b').subscribe();

    const req = http.expectOne(`${base}/queues/cola%20a%2Fb`);
    expect(req.request.method).toBe('GET');
    req.flush({ nombre: 'cola a/b', mensajes: 0, consumidores: 0 });
  });

  it('deletes a queue', () => {
    servicio.eliminarCola('cola.a').subscribe();

    const req = http.expectOne(`${base}/queues/cola.a`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('creates an exchange', () => {
    const cuerpo = { nombre: 'ex.a', tipo: 'topic' as const, durable: false };
    servicio.crearExchange(cuerpo).subscribe();

    const req = http.expectOne(`${base}/exchanges`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({});
  });

  it('deletes an exchange', () => {
    servicio.eliminarExchange('ex.a').subscribe();

    const req = http.expectOne(`${base}/exchanges/ex.a`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('creates a binding', () => {
    const cuerpo = { exchange: 'ex.a', destino: 'cola.a', tipoDestino: 'queue' as const, routingKey: 'k' };
    servicio.crearBinding(cuerpo).subscribe();

    const req = http.expectOne(`${base}/bindings`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({});
  });

  it('deletes a binding sending the request in the body', () => {
    const cuerpo = { exchange: 'ex.a', destino: 'cola.a' };
    servicio.eliminarBinding(cuerpo).subscribe();

    const req = http.expectOne(`${base}/bindings`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toEqual(cuerpo);
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
