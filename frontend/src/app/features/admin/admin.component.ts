import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  BffService,
  PedidoResponseDto,
  UsuarioDto,
  EnvioRequestDto,
} from '../../core/services/bff.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin.component.html',
})
export class AdminComponent {
  private readonly auth = inject(AuthService);
  private readonly bff = inject(BffService);
  private readonly fb = inject(FormBuilder);

  protected readonly usuarios = signal<UsuarioDto[]>([]);
  protected readonly pedidos = signal<PedidoResponseDto[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly feedback = signal('');

  protected readonly envioForm = this.fb.nonNullable.group({
    pedidoId: ['', [Validators.required]],
    repartidorId: ['', [Validators.required]],
    fechaEntregaEstimada: [''],
  });

  protected readonly adminChecklist = [
    'Gestion completa de usuarios desde el BFF.',
    'Visualización consolidada de pedidos para crear envíos.',
    'Formulario de asignación de envío desde una sola pantalla.',
    'Interfaz sobria para no mezclar operación y configuración.',
  ];

  protected readonly endpoints = ['GET /bff/usuarios', 'GET /bff/pedidos', 'POST /bff/envios'];

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated() && this.auth.hasAnyRole(['ADMIN'])) {
        void this.cargarDatos();
      } else {
        this.usuarios.set([]);
        this.pedidos.set([]);
      }
    });
  }

  async cargarDatos(): Promise<void> {
    if (this.loading()) {
      return;
    }

    this.loading.set(true);
    this.feedback.set('');

    try {
      const [usuarios, pedidos] = await Promise.all([
        firstValueFrom(this.bff.obtenerUsuarios()),
        firstValueFrom(this.bff.obtenerPedidos()),
      ]);

      this.usuarios.set(usuarios);
      this.pedidos.set(pedidos);
    } catch {
      this.feedback.set('No se pudieron cargar usuarios y pedidos desde el BFF.');
    } finally {
      this.loading.set(false);
    }
  }

  async crearEnvio(): Promise<void> {
    if (!this.auth.hasAnyRole(['ADMIN']) || this.envioForm.invalid || this.saving()) {
      this.envioForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.feedback.set('');

    const raw = this.envioForm.getRawValue();
    const request: EnvioRequestDto = {
      pedidoId: raw.pedidoId,
      repartidorId: raw.repartidorId,
      fechaEntregaEstimada: raw.fechaEntregaEstimada || null,
    };

    try {
      await firstValueFrom(this.bff.crearEnvio(request));
      this.envioForm.reset({ pedidoId: '', repartidorId: '', fechaEntregaEstimada: '' });
      this.feedback.set('Envío creado correctamente.');
      await this.cargarDatos();
    } catch {
      this.feedback.set('No se pudo crear el envío. Verifica los datos y el permiso de admin.');
    } finally {
      this.saving.set(false);
    }
  }
}