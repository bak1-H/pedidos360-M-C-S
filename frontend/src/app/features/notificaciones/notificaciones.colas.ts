import { Cola, FlujoCola } from './notificaciones.models';

const SUFIJO_DLQ = '.dlq';

export function nombreBaseDeCola(nombre: string): string {
  if (nombre.endsWith(SUFIJO_DLQ)) {
    return nombre.slice(0, -SUFIJO_DLQ.length);
  }

  return nombre;
}

export function agruparColas(colas: readonly Cola[]): FlujoCola[] {
  const flujos = new Map<string, FlujoCola>();

  for (const cola of colas) {
    const base = nombreBaseDeCola(cola.nombre);
    const flujo = flujos.get(base) ?? { base, negocio: null, dlq: null };

    if (cola.categoria === 'NEGOCIO') {
      flujo.negocio = cola;
    } else {
      flujo.dlq = cola;
    }

    flujos.set(base, flujo);
  }

  return [...flujos.values()].sort((a, b) => a.base.localeCompare(b.base));
}
