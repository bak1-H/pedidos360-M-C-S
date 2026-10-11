import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  BffService,
  CrearPedidoRequest,
  PedidoConTrackingResponse,
  PedidoResponseDto,
} from '../../core/services/bff.service';
import { cargaEncolada } from '../../shared/carga-encolada';
import { etiquetaEstado, fechaLegible, referenciaCorta } from '../../shared/referencia';
import {
  EnviosCargados,
  EnviosPorPedido,
  cargarEnviosPorPedido,
  claseEstado,
  enviosDesdeListado,
  estadoEfectivo,
} from '../../shared/seguimiento';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './pedidos.component.html',
})
export class PedidosComponent {
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private destruido = false;
  private readonly bff = inject(BffService);
  private readonly fb = inject(FormBuilder);

  protected readonly pedidos = signal<PedidoResponseDto[]>([]);
  protected readonly selectedPedido = signal<PedidoConTrackingResponse | null>(null);
  protected readonly envios = signal<EnviosPorPedido>({});
  protected readonly loading = signal(false);
  protected readonly cargandoDetalle = signal(false);
  protected readonly submitting = signal(false);
  protected readonly feedback = signal('');
  protected readonly error = signal('');
  protected readonly errorCarga = signal('');

  protected readonly isAdmin = () => this.auth.hasAnyRole(['ADMIN']);
  protected readonly puedeCrear = () => this.auth.hasAnyRole(['CLIENTE']);

  protected readonly enCamino = computed(() => this.contar('EN_TRANSITO'));
  protected readonly entregados = computed(() => this.contar('ENTREGADO'));
  protected readonly fallidos = computed(() => this.contar('FALLIDO'));
  protected readonly otros = computed(
    () => this.pedidos().length - this.enCamino() - this.entregados() - this.fallidos(),
  );

  protected readonly tiposPaquete = [
    { valor: 'DOCUMENTO', etiqueta: 'Documento' },
    { valor: 'CAJA', etiqueta: 'Caja' },
    { valor: 'FRAGIL', etiqueta: 'Frágil' },
  ];

  protected readonly pedidoForm = this.fb.nonNullable.group({
    direccionOrigen: ['', [Validators.required, Validators.minLength(5)]],
    direccionDestino: ['', [Validators.required, Validators.minLength(5)]],
    descripcion: ['', [Validators.required, Validators.minLength(3)]],
    cantidad: [1, [Validators.required, Validators.min(1)]],
    valorDeclarado: [15000, [Validators.required, Validators.min(0)]],
    pesoKg: [1, [Validators.required, Validators.min(0.1)]],
    alturaCm: [10, [Validators.required, Validators.min(1)]],
    anchoCm: [10, [Validators.required, Validators.min(1)]],
    largoCm: [10, [Validators.required, Validators.min(1)]],
    tipoPaquete: ['CAJA', [Validators.required]],
  });

  protected readonly referencia = referenciaCorta;
  protected readonly fecha = fechaLegible;
  protected readonly estadoLegible = etiquetaEstado;
  protected readonly claseBadge = claseEstado;
  protected readonly estadoEfectivo = estadoEfectivo;

  readonly cargarPedidos = cargaEncolada(() => this.cargarUnaVez());

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.destruido = true;
    });

    effect(() => {
      const authenticated = this.auth.isAuthenticated();

      untracked(() => {
        if (authenticated) {
          void this.cargarPedidos();
        } else {
          this.pedidos.set([]);
          this.envios.set({});
          this.selectedPedido.set(null);
        }
      });
    });
  }

  estadoDe(pedido: PedidoResponseDto): string {
    return estadoEfectivo(pedido, this.envios()[pedido.id]);
  }

  async crearPedido(): Promise<void> {
    if (this.pedidoForm.invalid || this.submitting()) {
      this.pedidoForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.feedback.set('');
    this.error.set('');

    const raw = this.pedidoForm.getRawValue();
    const request: CrearPedidoRequest = {
      direccionOrigen: raw.direccionOrigen,
      direccionDestino: raw.direccionDestino,
      detalles: [
        {
          descripcion: raw.descripcion,
          cantidad: raw.cantidad,
          valorDeclarado: raw.valorDeclarado,
        },
      ],
      paquete: {
        pesoKg: raw.pesoKg,
        alturaCm: raw.alturaCm,
        anchoCm: raw.anchoCm,
        largoCm: raw.largoCm,
        tipo: raw.tipoPaquete as CrearPedidoRequest['paquete']['tipo'],
      },
    };

    try {
      await firstValueFrom(this.bff.crearPedido(request));
      this.pedidoForm.reset({
        direccionOrigen: '',
        direccionDestino: '',
        descripcion: '',
        cantidad: 1,
        valorDeclarado: 15000,
        pesoKg: 1,
        alturaCm: 10,
        anchoCm: 10,
        largoCm: 10,
        tipoPaquete: 'CAJA',
      });
      this.feedback.set('Pedido creado. Ya puedes seguir su estado en la lista.');
      await this.cargarPedidos();
    } catch {
      this.error.set('No pudimos crear el pedido. Revisa los datos e intenta de nuevo.');
    } finally {
      this.submitting.set(false);
    }
  }

  async verDetalle(id: string): Promise<void> {
    if (this.cargandoDetalle()) {
      return;
    }

    this.cargandoDetalle.set(true);
    this.error.set('');

    try {
      this.selectedPedido.set(await firstValueFrom(this.bff.obtenerPedido(id)));
    } catch {
      this.error.set('No pudimos cargar el seguimiento de este pedido.');
    } finally {
      this.cargandoDetalle.set(false);
    }
  }

  cerrarDetalle(): void {
    this.selectedPedido.set(null);
  }

  private async cargarUnaVez(): Promise<void> {
    if (this.destruido) {
      return;
    }

    this.loading.set(true);

    try {
      const { pedidos, seguimiento } = await this.obtenerPedidosConEnvios();

      if (this.destruido) {
        return;
      }

      this.pedidos.set(pedidos);
      this.envios.set(seguimiento.envios);
      this.errorCarga.set(
        seguimiento.sinVerificar.length
          ? 'No pudimos verificar el envío de algunos pedidos; se muestra su estado registrado. Actualiza para volver a intentarlo.'
          : '',
      );
      await this.refrescarDetalleAbierto();
    } catch {
      if (!this.destruido) {
        this.errorCarga.set('No pudimos cargar tus pedidos. Intenta nuevamente.');
      }
    } finally {
      if (!this.destruido) {
        this.loading.set(false);
      }
    }
  }

  private async obtenerPedidosConEnvios(): Promise<{ pedidos: PedidoResponseDto[]; seguimiento: EnviosCargados }> {
    if (this.isAdmin()) {
      const [pedidos, envios] = await Promise.all([
        firstValueFrom(this.bff.obtenerPedidos()),
        firstValueFrom(this.bff.obtenerEnvios()),
      ]);

      return { pedidos, seguimiento: { envios: enviosDesdeListado(pedidos, envios), sinVerificar: [] } };
    }

    const pedidos = await firstValueFrom(this.bff.obtenerMisPedidos());

    return { pedidos, seguimiento: await cargarEnviosPorPedido(this.bff, pedidos, () => this.destruido) };
  }

  private contar(estado: string): number {
    return this.pedidos().filter((pedido) => this.estadoDe(pedido) === estado).length;
  }

  private async refrescarDetalleAbierto(): Promise<void> {
    const abierto = this.selectedPedido();

    if (!abierto) {
      return;
    }

    try {
      const actualizado = await firstValueFrom(this.bff.obtenerPedido(abierto.pedido.id));

      if (!this.destruido) {
        this.selectedPedido.set(actualizado);
      }
    } catch {
      if (!this.destruido) {
        this.selectedPedido.set(null);
      }
    }
  }
}
