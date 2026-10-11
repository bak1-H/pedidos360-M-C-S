import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { agruparColas } from './notificaciones.colas';
import {
  CategoriaCola,
  Cola,
  ESTADOS_NOTIFICACION,
  EstadoNotificacion,
  FlujoCola,
  PaginaNotificaciones,
  ResumenNotificaciones,
  TIPOS_NOTIFICACION,
  TipoNotificacion,
} from './notificaciones.models';
import { NotificacionesService } from './notificaciones.service';

const INTERVALO_AUTO_MS = 5000;
const TAMANO_PAGINA = 20;

const ETIQUETAS_TIPO: Record<string, string> = {
  PEDIDO_CREADO: 'Pedido creado',
};

const ETIQUETAS_ESTADO: Record<EstadoNotificacion, string> = {
  PROCESADA: 'Procesada',
  MUERTA: 'Muerta',
};

const ETIQUETAS_CATEGORIA: Record<CategoriaCola, string> = {
  NEGOCIO: 'Cola de negocio',
  DLQ: 'Mensajes muertos (DLQ)',
};

type ErrorCarga = 'sin-permiso' | 'generico' | null;

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  templateUrl: './notificaciones.component.html',
  styleUrl: './notificaciones.component.scss',
})
export class NotificacionesComponent {
  private readonly auth = inject(AuthService);
  private readonly servicio = inject(NotificacionesService);
  private readonly destroyRef = inject(DestroyRef);

  private recargaPendiente = false;
  private ultimoIntento = 0;

  protected readonly tipos = TIPOS_NOTIFICACION;
  protected readonly estados = ESTADOS_NOTIFICACION;

  protected readonly filtroTipo = signal<TipoNotificacion | ''>('');
  protected readonly filtroEstado = signal<EstadoNotificacion | ''>('');
  protected readonly numeroPagina = signal(0);

  protected readonly pagina = signal<PaginaNotificaciones | null>(null);
  protected readonly resumen = signal<ResumenNotificaciones | null>(null);
  protected readonly colas = signal<Cola[]>([]);

  protected readonly cargando = signal(false);
  protected readonly error = signal<ErrorCarga>(null);
  protected readonly autoActualizar = signal(true);
  protected readonly ultimaActualizacion = signal<Date | null>(null);
  private readonly ahora = signal(new Date());

  private readonly esAdmin = computed(
    () => this.auth.isAuthenticated() && this.auth.hasAnyRole(['ADMIN']),
  );

  protected readonly flujos = computed(() => agruparColas(this.colas()));

  protected readonly filas = computed(() => this.pagina()?.contenido ?? []);

  protected readonly totalPaginas = computed(() => Math.max(this.pagina()?.totalPaginas ?? 0, 1));

  protected readonly chipsTipo = computed(() => {
    const porTipo = this.resumen()?.porTipo ?? {};
    const claves = [...new Set<string>([...TIPOS_NOTIFICACION, ...Object.keys(porTipo)])];
    return claves.map((clave) => ({ clave, cantidad: porTipo[clave] ?? 0 }));
  });

  protected readonly segundosDesdeActualizacion = computed(() => {
    const ultima = this.ultimaActualizacion();
    if (!ultima) {
      return null;
    }

    return Math.max(0, Math.floor((this.ahora().getTime() - ultima.getTime()) / 1000));
  });

  constructor() {
    effect(() => {
      const esAdmin = this.esAdmin();

      untracked(() => {
        if (esAdmin) {
          void this.actualizar();
        } else {
          this.pagina.set(null);
          this.resumen.set(null);
          this.colas.set([]);
        }
      });
    });

    const reloj = setInterval(() => {
      this.ahora.set(new Date());
      this.refrescarAutomaticamente();
    }, 1000);
    this.destroyRef.onDestroy(() => clearInterval(reloj));
  }

  async actualizar(): Promise<void> {
    if (this.cargando()) {
      this.recargaPendiente = true;
      return;
    }

    this.cargando.set(true);

    try {
      const [pagina, resumen, colas] = await Promise.all([
        firstValueFrom(
          this.servicio.listar({
            tipo: this.filtroTipo(),
            estado: this.filtroEstado(),
            pagina: this.numeroPagina(),
            tamano: TAMANO_PAGINA,
          }),
        ),
        firstValueFrom(this.servicio.resumen()),
        firstValueFrom(this.servicio.colas()),
      ]);

      this.pagina.set(pagina);
      this.resumen.set(resumen);
      this.colas.set(colas);
      this.error.set(null);
      this.ultimaActualizacion.set(new Date());
      this.ahora.set(new Date());
    } catch (fallo) {
      this.error.set(fallo instanceof HttpErrorResponse && fallo.status === 403 ? 'sin-permiso' : 'generico');
    } finally {
      this.ultimoIntento = Date.now();
      this.cargando.set(false);

      if (this.recargaPendiente) {
        this.recargaPendiente = false;
        void this.actualizar();
      }
    }
  }

  protected alternarAutoActualizar(): void {
    this.autoActualizar.update((activo) => !activo);
  }

  protected cambiarTipo(valor: string): void {
    this.filtroTipo.set(valor as TipoNotificacion | '');
    this.numeroPagina.set(0);
    void this.actualizar();
  }

  protected cambiarEstado(valor: string): void {
    this.filtroEstado.set(valor as EstadoNotificacion | '');
    this.numeroPagina.set(0);
    void this.actualizar();
  }

  protected irAPagina(destino: number): void {
    if (destino < 0 || destino >= this.totalPaginas()) {
      return;
    }

    this.numeroPagina.set(destino);
    void this.actualizar();
  }

  protected etiquetaTipo(tipo: string): string {
    return ETIQUETAS_TIPO[tipo] ?? tipo;
  }

  protected claseTipo(tipo: string): string {
    return `tipo-${tipo.toLowerCase().replaceAll('_', '-')}`;
  }

  protected etiquetaEstado(estado: EstadoNotificacion): string {
    return ETIQUETAS_ESTADO[estado] ?? estado;
  }

  protected etiquetaCategoria(categoria: CategoriaCola): string {
    return ETIQUETAS_CATEGORIA[categoria];
  }

  protected fecha(valor: string): string {
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) {
      return 'Sin fecha';
    }

    return fecha.toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'medium' });
  }

  protected nodos(flujo: FlujoCola): { categoria: CategoriaCola; cola: Cola | null }[] {
    return [
      { categoria: 'NEGOCIO', cola: flujo.negocio },
      { categoria: 'DLQ', cola: flujo.dlq },
    ];
  }

  protected claseNodo(categoria: CategoriaCola, cola: Cola | null): string {
    const base = `nodo nodo-${categoria.toLowerCase()}`;
    return cola && cola.mensajes > 0 ? `${base} con-mensajes` : base;
  }

  protected estadoNodo(categoria: CategoriaCola, cola: Cola): string {
    if (cola.mensajes === 0) {
      return 'Vacía';
    }

    return categoria === 'NEGOCIO' ? 'En proceso' : 'Con fallos';
  }

  protected textoMensajes(cola: Cola): string {
    return cola.mensajes === 1 ? '1 mensaje' : `${cola.mensajes} mensajes`;
  }

  protected textoConsumidores(cola: Cola): string {
    return cola.consumidores === 1 ? '1 consumidor' : `${cola.consumidores} consumidores`;
  }

  private refrescarAutomaticamente(): void {
    if (!this.esAdmin() || !this.autoActualizar() || this.cargando() || this.error() === 'sin-permiso') {
      return;
    }

    if (Date.now() - this.ultimoIntento >= INTERVALO_AUTO_MS) {
      void this.actualizar();
    }
  }
}
