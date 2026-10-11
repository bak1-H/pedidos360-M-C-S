export function cargaEncolada(cargar: () => Promise<void>): () => Promise<void> {
  let enCurso: Promise<void> | null = null;
  let pendiente = false;

  const ciclo = async (): Promise<void> => {
    do {
      pendiente = false;
      await cargar();
    } while (pendiente);
  };

  return () => {
    if (enCurso) {
      pendiente = true;
      return enCurso;
    }

    enCurso = ciclo().finally(() => {
      enCurso = null;
    });
    return enCurso;
  };
}
