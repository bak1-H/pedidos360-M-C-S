import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  BffService,
  CrearPedidoRequest,
  PedidoConTrackingResponse,
  PedidoResponseDto,
} from '../../core/services/bff.service';
import { etiquetaEstado, fechaLegible, referenciaCorta } from '../../shared/referencia';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './pedidos.component.html',
})
export class PedidosComponent {
  private readonly auth = inject(AuthService);
  private readonly bff = inject(BffService);
  private readonly fb = inject(FormBuilder);

  protected readonly pedidos = signal<PedidoResponseDto[]>([]);
  protected readonly selectedPedido = signal<PedidoConTrackingResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly feedback = signal('');
  protected readonly error = signal('');

  protected readonly isAdmin = () => this.auth.hasAnyRole(['ADMIN']);
  protected readonly puedeCrear = () => this.auth.hasAnyRole(['CLIENTE']);

  protected readonly enCamino = computed(
    () => this.pedidos().filter((pedido) => pedido.estado === 'DESPACHADO').length,
  );
  protected readonly entregados = computed(
    () => this.pedidos().filter((pedido) => pedido.estado === 'ENTREGADO').length,
  );

  protected readonly tiposPaquete = [
    { valor: 'DOCUMENTO', etiqueta: 'Documento' },
    { valor: 'CAJA', etiqueta: 'Caja' },
    { valor: 'FRAGIL', etiqueta: 'Fragil' },
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

  constructor() {
    effect(() => {
      const authenticated = this.auth.isAuthenticated();

      untracked(() => {
        if (authenticated) {
          void this.cargarPedidos();
        } else {
          this.pedidos.set([]);
          this.selectedPedido.set(null);
        }
      });
    });
  }

  async cargarPedidos(): Promise<void> {
    if (this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const pedidos = this.isAdmin()
        ? await firstValueFrom(this.bff.obtenerPedidos())
        : await firstValueFrom(this.bff.obtenerMisPedidos());

      this.pedidos.set(pedidos);
    } catch {
      this.error.set('No pudimos cargar tus pedidos. Intenta nuevamente.');
    } finally {
      this.loading.set(false);
    }
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
    this.error.set('');

    try {
      this.selectedPedido.set(await firstValueFrom(this.bff.obtenerPedido(id)));
    } catch {
      this.error.set('No pudimos cargar el seguimiento de este pedido.');
    }
  }

  cerrarDetalle(): void {
    this.selectedPedido.set(null);
  }
}
