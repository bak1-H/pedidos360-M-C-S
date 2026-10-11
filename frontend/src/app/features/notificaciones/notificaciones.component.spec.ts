import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { NotificacionesComponent } from './notificaciones.component';
import {
  Cola,
  PaginaNotificaciones,
  ResumenNotificaciones,
} from './notificaciones.models';
import { NotificacionesService } from './notificaciones.service';

const authAdmin = {
  isAuthenticated: () => true,
  hasAnyRole: () => true,
};

const resumen: ResumenNotificaciones = {
  total: 7,
  porTipo: { PEDIDO_CREADO: 7 },
  porEstado: { PROCESADA: 5, MUERTA: 2 },
};

const colas: Cola[] = [
  { nombre: 'pedido.creado', categoria: 'NEGOCIO', mensajes: 2, consumidores: 1 },
  { nombre: 'pedido.creado.dlq', categoria: 'DLQ', mensajes: 1, consumidores: 0 },
];

const paginaConMuerta: PaginaNotificaciones = {
  contenido: [
    {
      id: '1',
      tipo: 'PEDIDO_CREADO',
      estado: 'MUERTA',
      routingKey: 'pedido.creado',
      cola: 'pedido.creado.dlq',
      destinatario: 'cliente@correo.com',
      mensaje: 'Mensaje fallido',
      referenciaId: null,
      fechaRecepcion: '2026-01-01T10:00:00Z',
    },
  ],
  pagina: 0,
  tamano: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

const paginaVacia: PaginaNotificaciones = {
  contenido: [],
  pagina: 0,
  tamano: 20,
  totalElementos: 0,
  totalPaginas: 0,
};

async function crear(pagina: PaginaNotificaciones): Promise<HTMLElement> {
  TestBed.configureTestingModule({
    imports: [NotificacionesComponent],
    providers: [
      { provide: AuthService, useValue: authAdmin },
      {
        provide: NotificacionesService,
        useValue: {
          listar: () => of(pagina),
          resumen: () => of(resumen),
          colas: () => of(colas),
        },
      },
    ],
  });

  const fixture = TestBed.createComponent(NotificacionesComponent);
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('NotificacionesComponent', () => {
  it('renders the summary and the queue flow', async () => {
    const html = await crear(paginaConMuerta);
    const texto = html.textContent ?? '';

    expect(texto).toContain('7');
    expect(texto).toContain('Pedido creado');
    expect(texto).toContain('Procesadas');
    expect(texto).toContain('Muertas');
    expect(html.querySelectorAll('.flujo').length).toBe(1);
    expect(html.querySelectorAll('.nodo').length).toBe(2);
    expect(html.querySelector('.nodo-dlq.con-mensajes')).not.toBeNull();
    expect(html.querySelector('.nodo-negocio.con-mensajes')).not.toBeNull();
  });

  it('renders a MUERTA row with text and highlight', async () => {
    const html = await crear(paginaConMuerta);
    const fila = html.querySelector('tbody tr');

    expect(fila).not.toBeNull();
    expect(fila?.classList.contains('fila-muerta')).toBe(true);
    expect(fila?.textContent).toContain('Muerta');
    expect(fila?.textContent).toContain('cliente@correo.com');
  });

  it('renders the empty state when there are no notifications', async () => {
    const html = await crear(paginaVacia);

    expect(html.querySelector('tbody')).toBeNull();
    expect(html.textContent).toContain('No hay notificaciones para mostrar.');
  });
});
