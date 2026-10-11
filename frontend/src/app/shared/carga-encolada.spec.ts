import { cargaEncolada } from './carga-encolada';

describe('cargaEncolada', () => {
  it('runs once when idle', async () => {
    const cargar = vi.fn(async () => undefined);

    await cargaEncolada(cargar)();

    expect(cargar).toHaveBeenCalledTimes(1);
  });

  it('queues a single follow-up run for any number of calls made during a load', async () => {
    const liberaciones: (() => void)[] = [];
    const cargar = vi.fn(() => new Promise<void>((resolver) => liberaciones.push(resolver)));
    const recargar = cargaEncolada(cargar);

    const primera = recargar();
    const segunda = recargar();
    const tercera = recargar();
    expect(cargar).toHaveBeenCalledTimes(1);

    liberaciones[0]();
    await vi.waitFor(() => expect(cargar).toHaveBeenCalledTimes(2));
    liberaciones[1]();
    await Promise.all([primera, segunda, tercera]);

    expect(cargar).toHaveBeenCalledTimes(2);
  });

  it('does not stay stuck when cargar throws synchronously', async () => {
    const cargar = vi
      .fn<() => Promise<void>>()
      .mockImplementationOnce(() => {
        throw new Error('falla sincrona');
      })
      .mockResolvedValue(undefined);
    const recargar = cargaEncolada(cargar);

    await expect(recargar()).rejects.toThrow('falla sincrona');
    await recargar();

    expect(cargar).toHaveBeenCalledTimes(2);
  });

  it('accepts a new run after the queue drained', async () => {
    const cargar = vi.fn(async () => undefined);
    const recargar = cargaEncolada(cargar);

    await recargar();
    await recargar();

    expect(cargar).toHaveBeenCalledTimes(2);
  });
});
