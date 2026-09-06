import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  BffService,
  EnvioRequestDto,
  PedidoResponseDto,
  UsuarioDto,
} from '../../core/services/bff.service';
import { etiquetaEstado, fechaLegible, referenciaCorta } from '../../shared/referencia';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [ReactiveFormsModule],
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
  protected readonly error = signal('');

  protected readonly repartidores = computed(() =>
    this.usuarios().filter((usuario) => usuario.rol === 'REPARTIDOR'),
  );

  protected readonly pedidosSinDespachar = computed(() =>
    this.pedidos().filter((pedido) => pedido.estado === 'CREADO'),
  );

  protected readonly envioForm = this.fb.nonNullable.group({
    pedidoId: ['', [Validators.required]],
    repartidorId: ['', [Validators.required]],
    fechaEntregaEstimada: [''],
  });

  protected readonly referencia = referenciaCorta;
  protected readonly fecha = fechaLegible;
  protected readonly estadoLegible = etiquetaEstado;

  constructor() {
    effect(() => {
      const esAdmin = this.auth.isAuthenticated() && this.auth.hasAnyRole(['ADMIN']);

      untracked(() => {
        if (esAdmin) {
          void this.cargarDatos();
        } else {
          this.usuarios.set([]);
          this.pedidos.set([]);
        }
      });
    });
  }

  async cargarDatos(): Promise<void> {
    if (this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const [usuarios, pedidos] = await Promise.all([
        firstValueFrom(this.bff.obtenerUsuarios()),
        firstValueFrom(this.bff.obtenerPedidos()),
      ]);

      this.usuarios.set(usuarios);
      this.pedidos.set(pedidos);
    } catch {
      this.error.set('No pudimos cargar la informacion de la operacion. Intenta nuevamente.');
    } finally {
      this.loading.set(false);
    }
  }

  etiquetaPedido(pedido: PedidoResponseDto): string {
    return `${referenciaCorta(pedido.id)} · ${pedido.direccionOrigen} → ${pedido.direccionDestino}`;
  }

  async crearEnvio(): Promise<void> {
    if (this.envioForm.invalid || this.saving()) {
      this.envioForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set('');
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
      this.feedback.set('Envio asignado correctamente.');
      await this.cargarDatos();
    } catch {
      this.error.set('No pudimos asignar el envio. Verifica que el pedido no tenga uno ya asignado.');
    } finally {
      this.saving.set(false);
    }
  }
}
