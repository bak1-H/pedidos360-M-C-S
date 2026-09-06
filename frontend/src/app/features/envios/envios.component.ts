import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BffService, CambioEstadoEnvioDto, EnvioResponseDto } from '../../core/services/bff.service';
import { etiquetaEstado, fechaLegible, referenciaCorta } from '../../shared/referencia';

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

  protected readonly puedeVerSusEnvios = () => this.auth.hasAnyRole(['REPARTIDOR']);

  protected readonly enTransito = computed(
    () => this.envios().filter((envio) => envio.estadoEnvio === 'EN_TRANSITO').length,
  );
  protected readonly entregados = computed(
    () => this.envios().filter((envio) => envio.estadoEnvio === 'ENTREGADO').length,
  );

  protected readonly estados = [
    { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
    { valor: 'EN_TRANSITO', etiqueta: 'En transito' },
    { valor: 'ENTREGADO', etiqueta: 'Entregado' },
    { valor: 'FALLIDO', etiqueta: 'Fallido' },
  ];

  protected readonly cambioEstadoForm = this.fb.nonNullable.group({
    estadoEnvio: ['EN_TRANSITO', [Validators.required]],
    descripcionEvento: ['', [Validators.required, Validators.minLength(4)]],
  });

  protected readonly referencia = referenciaCorta;
  protected readonly fecha = fechaLegible;
  protected readonly estadoLegible = etiquetaEstado;

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

  async cargarEnvios(): Promise<void> {
    if (this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      this.envios.set(await firstValueFrom(this.bff.obtenerMisEnvios()));
    } catch {
      this.error.set('No pudimos cargar tus envios asignados. Intenta nuevamente.');
    } finally {
      this.loading.set(false);
    }
  }

  seleccionarEnvio(envio: EnvioResponseDto): void {
    this.envioSeleccionado.set(envio);
    this.feedback.set('');
    this.cambioEstadoForm.reset({
      estadoEnvio: envio.estadoEnvio === 'PENDIENTE' ? 'EN_TRANSITO' : envio.estadoEnvio,
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

    this.saving.set(true);
    this.error.set('');

    const raw = this.cambioEstadoForm.getRawValue();
    const request: CambioEstadoEnvioDto = {
      estadoEnvio: raw.estadoEnvio as CambioEstadoEnvioDto['estadoEnvio'],
      descripcionEvento: raw.descripcionEvento,
    };

    try {
      await firstValueFrom(this.bff.cambiarEstadoEnvio(envio.id, request));
      this.feedback.set(`Envio ${referenciaCorta(envio.id)} actualizado a ${etiquetaEstado(request.estadoEnvio)}.`);
      this.envioSeleccionado.set(null);
      await this.cargarEnvios();
    } catch {
      this.error.set('No pudimos actualizar el envio. Revisa que siga asignado a ti.');
    } finally {
      this.saving.set(false);
    }
  }
}
