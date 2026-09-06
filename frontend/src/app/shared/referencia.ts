export function referenciaCorta(id: string | null | undefined): string {
  if (!id) {
    return 'Sin asignar';
  }

  return `#${id.slice(0, 8).toUpperCase()}`;
}

export function fechaLegible(valor: string | null | undefined, vacio = 'Sin definir'): string {
  if (!valor) {
    return vacio;
  }

  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return vacio;
  }

  return fecha.toLocaleString('es-CL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function etiquetaEstado(estado: string): string {
  const etiquetas: Record<string, string> = {
    CREADO: 'Creado',
    DESPACHADO: 'Despachado',
    ENTREGADO: 'Entregado',
    CANCELADO: 'Cancelado',
    PENDIENTE: 'Pendiente',
    EN_TRANSITO: 'En tránsito',
    FALLIDO: 'Fallido',
  };

  return etiquetas[estado] ?? estado;
}
