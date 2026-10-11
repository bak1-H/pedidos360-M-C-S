import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BffService, CambioEstadoEnvioDto, EnvioResponseDto } from '../../core/services/bff.service';
import { cargaEncolada } from '../../shared/carga-encolada';
import { codigoDeError } from '../../shared/errores';
import { etiquetaEstado, fechaLegible, referenciaCorta } from '../../shared/referencia';
import { claseEstado, envioFinalizado, transicionesPermitidas } from '../../shared/seguimiento';

@Component({
  selector: 'app-envios',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './envios.component.html',
})
export class EnviosComponent {
  private readonly auth = inject(AuthService);
  private readonly bff = inject(BffService);
  private readonly fb = inject(FormBuilder);

  protected readonly envios = signal<EnvioResponseDto[]>([]);
  protected readonly envioSeleccionado = signal<EnvioResponseDto | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly feedback = signal('');
  protected readonly error = signal('');
  protected readonly errorCarga = signal('');

  protected readonly puedeVerSusEnvios = () => this.auth.hasAnyRole(['REPARTIDOR']);

  protected readonly pendientes = computed(() => this.contar('PENDIENTE'));
  protected readonly enTransito = computed(() => this.contar('EN_TRANSITO'));
  protected readonly entregados = computed(() => this.contar('ENTREGADO'));
  protected readonly fallidos = computed(() => this.contar('FALLIDO'));

  protected readonly enviosOrdenados = computed(() =>
    [...this.envios()].sort(
      (a, b) => Number(envioFinalizado(a.estadoEnvio)) - Number(envioFinalizado(b.estadoEnvio)),
    ),
  );

  protected readonly estados = computed(() => {
    const envio = this.envioSeleccionado();

    return envio
      ? transicionesPermitidas(envio.estadoEnvio).map((valor) => ({ valor, etiqueta: etiquetaEstado(valor) }))
      : [];
  });

  protected readonly cambioEstadoForm = this.fb.nonNullable.group({
    estadoEnvio: ['', [Validators.required]],
    descripcionEvento: ['', [Validators.required, Validators.minLength(4)]],
  });

  protected readonly referencia = referenciaCorta;
  protected readonly fecha = fechaLegible;
  protected readonly estadoLegible = etiquetaEstado;
  protected readonly claseBadge = claseEstado;
  protected readonly finalizado = envioFinalizado;

  readonly cargarEnvios = cargaEncolada(() => this.cargarUnaVez());

  constructor() {
    effect(() => {
      const habilitado = this.auth.isAuthenticated() && this.puedeVerSusEnvios();

      untracked(() => {
        if (habilitado) {
          void this.cargarEnvios();
        } else {
          this.envios.set([]);
        }
      });
    });
  }

  seleccionarEnvio(envio: EnvioResponseDto): void {
    const siguientes = transicionesPermitidas(envio.estadoEnvio);

    if (!siguientes.length) {
      return;
    }

    this.envioSeleccionado.set(envio);
    this.feedback.set('');
    this.error.set('');
    this.cambioEstadoForm.reset({
      estadoEnvio: siguientes[0],
      descripcionEvento: '',
    });
  }

  cancelarSeleccion(): void {
    this.envioSeleccionado.set(null);
    this.feedback.set('');
  }

  async guardarEstado(): Promise<void> {
    const envio = this.envioSeleccionado();

    if (!envio || this.cambioEstadoForm.invalid || this.saving()) {
      this.cambioEstadoForm.markAllAsTouched();
      return;
    }

    const raw = this.cambioEstadoForm.getRawValue();

    if (!transicionesPermitidas(envio.estadoEnvio).includes(raw.estadoEnvio as CambioEstadoEnvioDto['estadoEnvio'])) {
      return;
    }

    this.saving.set(true);
    this.error.set('');
    this.feedback.set('');
    const request: CambioEstadoEnvioDto = {
      estadoEnvio: raw.estadoEnvio as CambioEstadoEnvioDto['estadoEnvio'],
      descripcionEvento: raw.descripcionEvento,
    };

    try {
      await firstValueFrom(this.bff.cambiarEstadoEnvio(envio.id, request));
      this.feedback.set(`Envío ${referenciaCorta(envio.id)} actualizado a ${etiquetaEstado(request.estadoEnvio)}.`);
    } catch (causa) {
      this.error.set(this.mensajeDeFalloAlActualizar(causa));
    }

    this.envioSeleccionado.set(null);

    try {
      await this.cargarEnvios();
    } finally {
      this.saving.set(false);
    }
  }

  private async cargarUnaVez(): Promise<void> {
    this.loading.set(true);

    try {
      this.envios.set(await firstValueFrom(this.bff.obtenerMisEnvios()));
      this.errorCarga.set('');
    } catch {
      this.errorCarga.set('No pudimos cargar tus envíos asignados. Intenta nuevamente.');
    } finally {
      this.loading.set(false);
    }
  }

  private contar(estado: string): number {
    return this.envios().filter((envio) => envio.estadoEnvio === estado).length;
  }

  private mensajeDeFalloAlActualizar(causa: unknown): string {
    switch (codigoDeError(causa)) {
      case 'TRANSICION_INVALIDA':
        return 'El envío ya cambió de estado; se actualizó la lista.';
      case 'SIN_PERMISO':
        return 'Este envío ya no está asignado a ti. Actualizamos tu lista.';
      case 'NO_ENCONTRADO':
        return 'No encontramos ese envío. Actualizamos tu lista.';
      default:
        return 'No pudimos actualizar el envío. Intenta nuevamente; revisamos tu lista para mostrar su estado actual.';
    }
  }
}
