import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  BffService,
  CambioEstadoEnvioDto,
  EnvioRequestDto,
  EnvioResponseDto,
} from '../../core/services/bff.service';

@Component({
  selector: 'app-envios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './envios.component.html',
})
export class EnviosComponent {
  private readonly auth = inject(AuthService);
  private readonly bff = inject(BffService);
  private readonly fb = inject(FormBuilder);

  protected readonly envios = signal<EnvioResponseDto[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly feedback = signal('');

  protected readonly canViewOwnEnvios = () => this.auth.hasAnyRole(['REPARTIDOR']);
  protected readonly canManageEnvios = () => this.auth.hasAnyRole(['ADMIN', 'REPARTIDOR']);

  protected readonly crearEnvioForm = this.fb.nonNullable.group({
    pedidoId: ['', [Validators.required]],
    repartidorId: ['', [Validators.required]],
    fechaEntregaEstimada: [''],
  });

  protected readonly cambioEstadoForm = this.fb.nonNullable.group({
    envioId: ['', [Validators.required]],
    estadoEnvio: ['EN_TRANSITO', [Validators.required]],
    descripcionEvento: ['', [Validators.required, Validators.minLength(4)]],
  });

  protected readonly statusList = ['PENDIENTE', 'EN_TRANSITO', 'ENTREGADO', 'FALLIDO'];

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated() && this.canViewOwnEnvios()) {
        void this.cargarEnvios();
      } else {
        this.envios.set([]);
      }
    });
  }

  async cargarEnvios(): Promise<void> {
    if (this.loading()) {
      return;
    }

    if (!this.canViewOwnEnvios()) {
      this.feedback.set('Este panel lista envíos asignados al repartidor autenticado.');
      return;
    }

    this.loading.set(true);
    this.feedback.set('');

    try {
      this.envios.set(await firstValueFrom(this.bff.obtenerMisEnvios()));
    } catch {
      this.feedback.set('No se pudo cargar la lista de envíos asignados.');
    } finally {
      this.loading.set(false);
    }
  }

  async crearEnvio(): Promise<void> {
    if (!this.auth.hasAnyRole(['ADMIN']) || this.crearEnvioForm.invalid || this.saving()) {
      this.crearEnvioForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.feedback.set('');

    const raw = this.crearEnvioForm.getRawValue();
    const request: EnvioRequestDto = {
      pedidoId: raw.pedidoId,
      repartidorId: raw.repartidorId,
      fechaEntregaEstimada: raw.fechaEntregaEstimada || null,
    };

    try {
      await firstValueFrom(this.bff.crearEnvio(request));
      this.crearEnvioForm.reset({ pedidoId: '', repartidorId: '', fechaEntregaEstimada: '' });
      this.feedback.set('Envío creado correctamente.');
      await this.cargarEnvios();
    } catch {
      this.feedback.set('No se pudo crear el envío. Verifica pedido, repartidor y permisos.');
    } finally {
      this.saving.set(false);
    }
  }

  seleccionarEnvio(envio: EnvioResponseDto): void {
    this.cambioEstadoForm.patchValue({
      envioId: envio.id,
      estadoEnvio: envio.estadoEnvio as 'PENDIENTE' | 'EN_TRANSITO' | 'ENTREGADO' | 'FALLIDO',
      descripcionEvento: `Actualización de estado para ${envio.id.slice(0, 8)}`,
    });
  }

  async cambiarEstado(): Promise<void> {
    if (!this.canManageEnvios() || this.cambioEstadoForm.invalid || this.saving()) {
      this.cambioEstadoForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.feedback.set('');

    const raw = this.cambioEstadoForm.getRawValue();
    const request: CambioEstadoEnvioDto = {
      estadoEnvio: raw.estadoEnvio as CambioEstadoEnvioDto['estadoEnvio'],
      descripcionEvento: raw.descripcionEvento,
    };

    try {
      await firstValueFrom(this.bff.cambiarEstadoEnvio(raw.envioId, request));
      this.feedback.set('Estado del envío actualizado.');
      await this.cargarEnvios();
    } catch {
      this.feedback.set('No se pudo actualizar el envío.');
    } finally {
      this.saving.set(false);
    }
  }
}