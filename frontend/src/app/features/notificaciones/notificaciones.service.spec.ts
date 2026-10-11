import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { appSettings } from '../../core/config/app-settings';
import { NotificacionesService } from './notificaciones.service';

describe('NotificacionesService', () => {
  const base = `${appSettings.bffApiUrl.replace(/\/$/, '')}/notificaciones`;
  let servicio: NotificacionesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(NotificacionesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists with pagination only when no filters are set', () => {
    servicio.listar({ tipo: '', estado: '', pagina: 0, tamano: 20 }).subscribe();

    const req = http.expectOne((r) => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('pagina')).toBe('0');
    expect(req.request.params.get('tamano')).toBe('20');
    expect(req.request.params.keys().sort()).toEqual(['pagina', 'tamano']);
    req.flush({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });

  it('lists with tipo and estado filters', () => {
    servicio.listar({ tipo: 'PEDIDO_CREADO', estado: 'MUERTA', pagina: 2, tamano: 20 }).subscribe();

    const req = http.expectOne((r) => r.url === base);
    expect(req.request.params.get('tipo')).toBe('PEDIDO_CREADO');
    expect(req.request.params.get('estado')).toBe('MUERTA');
    expect(req.request.params.get('pagina')).toBe('2');
    req.flush({ contenido: [], pagina: 2, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });

  it('requests the summary', () => {
    servicio.resumen().subscribe();

    const req = http.expectOne(`${base}/resumen`);
    expect(req.request.method).toBe('GET');
    req.flush({ total: 0, porTipo: {}, porEstado: { PROCESADA: 0, MUERTA: 0 } });
  });

  it('requests the queues', () => {
    servicio.colas().subscribe();

    const req = http.expectOne(`${base}/colas`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
