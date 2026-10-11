import { agruparColas, nombreBaseDeCola } from './notificaciones.colas';
import { Cola } from './notificaciones.models';

const cola = (nombre: string, categoria: Cola['categoria'], mensajes = 0): Cola => ({
  nombre,
  categoria,
  mensajes,
  consumidores: 1,
});

describe('notificaciones.colas', () => {
  it('strips the dlq suffix', () => {
    expect(nombreBaseDeCola('flujo.uno')).toBe('flujo.uno');
    expect(nombreBaseDeCola('flujo.uno.dlq')).toBe('flujo.uno');
  });

  it('groups business and dlq queues by flow and sorts them', () => {
    const flujos = agruparColas([
      cola('a.flujo', 'NEGOCIO', 3),
      cola('b.flujo', 'NEGOCIO'),
      cola('a.flujo.dlq', 'DLQ', 1),
      cola('b.flujo.dlq', 'DLQ'),
    ]);

    expect(flujos.map((f) => f.base)).toEqual(['a.flujo', 'b.flujo']);
    expect(flujos[0].negocio?.mensajes).toBe(3);
    expect(flujos[0].dlq?.nombre).toBe('a.flujo.dlq');
    expect(flujos[1].negocio?.nombre).toBe('b.flujo');
  });

  it('leaves missing slots as null', () => {
    const [flujo] = agruparColas([cola('solo.flujo', 'NEGOCIO')]);

    expect(flujo.dlq).toBeNull();
  });
});
