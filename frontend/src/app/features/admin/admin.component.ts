import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  BffService,
  EnvioRequestDto,
  PedidoResponseDto,
  UsuarioDto,
} from '../../core/services/bff.service';
import { cargaEncolada } from '../../shared/carga-encolada';
import { codigoDeError } from '../../shared/errores';
import { etiquetaEstado, fechaLegible, referenciaCorta } from '../../shared/referencia';
import {
  EnviosPorPedido,
  GrupoEstado,
  claseEstado,
  enviosDesdeListado,
  esAsignable,
  estadoEfectivo,
  grupoEstado,
} from '../../shared/seguimiento';

export type FiltroPedidos = GrupoEstado | 'todos';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin.component.html',
})
export class AdminComponent {
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private destruido = false;
  private readonly bff = inject(BffService);
  private readonly fb = inject(FormBuilder);

  protected readonly usuarios = signal<UsuarioDto[]>([]);
  protected readonly pedidos = signal<PedidoResponseDto[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly feedback = signal('');
  protected readonly error = signal('');
  protected readonly errorCarga = signal('');

  protected readonly repartidores = computed(() =>
    this.usuarios().filter((usuario) => usuario.rol === 'REPARTIDOR'),
  );

  protected readonly envios = signal<EnviosPorPedido>({});
  protected readonly filtro = signal<FiltroPedidos>('todos');

  protected readonly filtros: { valor: FiltroPedidos; etiqueta: string }[] = [
    { valor: 'todos', etiqueta: 'Todos' },
    { valor: 'sin-asignar', etiqueta: 'Sin asignar' },
    { valor: 'en-curso', etiqueta: 'En curso' },
    { valor: 'finalizado', etiqueta: 'Finalizados' },
  ];

  protected readonly pedidosAsignables = computed(() =>
    this.pedidos().filter((pedido) => esAsignable(pedido, this.envios())),
  );

  protected readonly fallidos = computed(
    () => this.pedidos().filter((pedido) => this.estadoDe(pedido) === 'FALLIDO').length,
  );

  protected readonly pedidosFiltrados = computed(() => {
    const filtro = this.filtro();

    return this.pedidos().filter(
      (pedido) => filtro === 'todos' || grupoEstado(this.estadoDe(pedido)) === filtro,
    );
  });

  protected readonly envioForm = this.fb.nonNullable.group({
    pedidoId: ['', [Validators.required]],
    repartidorId: ['', [Validators.required]],
    fechaEntregaEstimada: [''],
  });

  protected readonly referencia = referenciaCorta;
  protected readonly fecha = fechaLegible;
  protected readonly estadoLegible = etiquetaEstado;
  protected readonly claseBadge = claseEstado;

  readonly cargarDatos = cargaEncolada(() => this.cargarUnaVez());

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.destruido = true;
    });

    effect(() => {
      const esAdmin = this.auth.isAuthenticated() && this.auth.hasAnyRole(['ADMIN']);

      untracked(() => {
        if (esAdmin) {
          void this.cargarDatos();
        } else {
          this.usuarios.set([]);
          this.pedidos.set([]);
          this.envios.set({});
        }
      });
    });
  }

  estadoDe(pedido: PedidoResponseDto): string {
    return estadoEfectivo(pedido, this.envios()[pedido.id]);
  }

  repartidorDe(pedido: PedidoResponseDto): string {
    const repartidorId = this.envios()[pedido.id]?.repartidorId;

    if (!repartidorId) {
      return 'Sin asignar';
    }

    return this.usuarios().find((usuario) => usuario.id === repartidorId)?.nombre ?? referenciaCorta(repartidorId);
  }

  seleccionarFiltro(valor: string): void {
    this.filtro.set(valor as FiltroPedidos);
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
      this.feedback.set(`Pedido ${referenciaCorta(request.pedidoId)} asignado correctamente.`);
    } catch (causa) {
      this.error.set(this.mensajeDeFalloAlAsignar(causa));
    }

    try {
      await this.cargarDatos();
    } finally {
      this.saving.set(false);
    }
  }

  private async cargarUnaVez(): Promise<void> {
    if (this.destruido) {
      return;
    }

    this.loading.set(true);

    try {
      const [usuarios, pedidos, envios] = await Promise.all([
        firstValueFrom(this.bff.obtenerUsuarios()),
        firstValueFrom(this.bff.obtenerPedidos()),
        firstValueFrom(this.bff.obtenerEnvios()),
      ]);

      if (this.destruido) {
        return;
      }

      this.usuarios.set(usuarios);
      this.pedidos.set(pedidos);
      this.envios.set(enviosDesdeListado(pedidos, envios));
      this.errorCarga.set('');
      this.descartarSeleccionObsoleta();
    } catch {
      if (!this.destruido) {
        this.errorCarga.set('No pudimos cargar la información de la operación. Intenta nuevamente.');
      }
    } finally {
      if (!this.destruido) {
        this.loading.set(false);
      }
    }
  }

  private mensajeDeFalloAlAsignar(causa: unknown): string {
    switch (codigoDeError(causa)) {
      case 'ENVIO_DUPLICADO':
        return 'Este pedido ya tiene un envío asignado.';
      case 'PEDIDO_NO_ASIGNABLE':
        return 'El pedido ya no está disponible para asignar.';
      default:
        return 'No pudimos asignar el envío. Intenta nuevamente.';
    }
  }

  private descartarSeleccionObsoleta(): void {
    const { pedidoId, repartidorId } = this.envioForm.getRawValue();

    if (pedidoId && !this.pedidosAsignables().some((pedido) => pedido.id === pedidoId)) {
      this.envioForm.controls.pedidoId.reset('');
    }

    if (repartidorId && !this.repartidores().some((repartidor) => repartidor.id === repartidorId)) {
      this.envioForm.controls.repartidorId.reset('');
    }
  }
}
