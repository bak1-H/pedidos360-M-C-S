import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, firstValueFrom } from 'rxjs';
import {
  ColaInfoResponse,
  PATRON_NOMBRE_RABBIT,
  TIPOS_DESTINO,
  TIPOS_EXCHANGE,
} from './rabbit-admin.models';
import { RabbitAdminService } from './rabbit-admin.service';

const MENSAJE_NOMBRE = 'Solo admite letras, números, punto, guion y guion bajo (máximo 255).';

const vacioAIndefinido = (valor: string): string | undefined => valor.trim() || undefined;

@Component({
  selector: 'app-rabbit-admin',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './rabbit-admin.component.html',
  styleUrl: './rabbit-admin.component.scss',
})
export class RabbitAdminComponent {
  private readonly servicio = inject(RabbitAdminService);
  private readonly fb = inject(FormBuilder);

  protected readonly tiposExchange = TIPOS_EXCHANGE;
  protected readonly tiposDestino = TIPOS_DESTINO;
  protected readonly mensajeNombre = MENSAJE_NOMBRE;

  protected readonly ocupado = signal(false);
  protected readonly exito = signal('');
  protected readonly error = signal('');
  protected readonly colaEncontrada = signal<ColaInfoResponse | null>(null);

  private readonly nombreObligatorio = [Validators.required, Validators.pattern(PATRON_NOMBRE_RABBIT)];

  protected readonly buscarColaForm = this.fb.nonNullable.group({
    nombre: ['', this.nombreObligatorio],
  });

  protected readonly crearColaForm = this.fb.nonNullable.group({
    nombre: ['', this.nombreObligatorio],
    durable: [true],
    deadLetterExchange: ['', [Validators.pattern(PATRON_NOMBRE_RABBIT)]],
    deadLetterRoutingKey: [''],
  });

  protected readonly crearExchangeForm = this.fb.nonNullable.group({
    nombre: ['', this.nombreObligatorio],
    tipo: ['direct' as (typeof TIPOS_EXCHANGE)[number], [Validators.required]],
    durable: [true],
  });

  protected readonly eliminarExchangeForm = this.fb.nonNullable.group({
    nombre: ['', this.nombreObligatorio],
  });

  protected readonly bindingForm = this.fb.nonNullable.group({
    exchange: ['', this.nombreObligatorio],
    destino: ['', this.nombreObligatorio],
    tipoDestino: ['queue' as (typeof TIPOS_DESTINO)[number]],
    routingKey: [''],
  });

  protected invalido(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  protected buscarCola(): Promise<void> {
    const { nombre } = this.buscarColaForm.getRawValue();

    return this.ejecutar(this.buscarColaForm, () => this.servicio.obtenerCola(nombre), (cola) => {
      this.colaEncontrada.set(cola);
      return '';
    });
  }

  protected eliminarCola(): Promise<void> {
    const { nombre } = this.buscarColaForm.getRawValue();

    return this.ejecutar(this.buscarColaForm, () => this.servicio.eliminarCola(nombre), () => {
      this.colaEncontrada.set(null);
      return `Cola ${nombre} eliminada.`;
    });
  }

  protected crearCola(): Promise<void> {
    const valores = this.crearColaForm.getRawValue();

    return this.ejecutar(
      this.crearColaForm,
      () =>
        this.servicio.crearCola({
          nombre: valores.nombre,
          durable: valores.durable,
          deadLetterExchange: vacioAIndefinido(valores.deadLetterExchange),
          deadLetterRoutingKey: vacioAIndefinido(valores.deadLetterRoutingKey),
        }),
      () => `Cola ${valores.nombre} creada.`,
    );
  }

  protected crearExchange(): Promise<void> {
    const valores = this.crearExchangeForm.getRawValue();

    return this.ejecutar(
      this.crearExchangeForm,
      () => this.servicio.crearExchange(valores),
      () => `Exchange ${valores.nombre} creado.`,
    );
  }

  protected eliminarExchange(): Promise<void> {
    const { nombre } = this.eliminarExchangeForm.getRawValue();

    return this.ejecutar(
      this.eliminarExchangeForm,
      () => this.servicio.eliminarExchange(nombre),
      () => `Exchange ${nombre} eliminado.`,
    );
  }

  protected crearBinding(): Promise<void> {
    return this.ejecutar(
      this.bindingForm,
      () => this.servicio.crearBinding(this.requestBinding()),
      () => 'Binding creado.',
    );
  }

  protected eliminarBinding(): Promise<void> {
    return this.ejecutar(
      this.bindingForm,
      () => this.servicio.eliminarBinding(this.requestBinding()),
      () => 'Binding eliminado.',
    );
  }

  private requestBinding() {
    const valores = this.bindingForm.getRawValue();

    return {
      exchange: valores.exchange,
      destino: valores.destino,
      tipoDestino: valores.tipoDestino,
      routingKey: vacioAIndefinido(valores.routingKey),
    };
  }

  private async ejecutar<T>(
    formulario: FormGroup,
    accion: () => Observable<T>,
    alExito: (respuesta: T) => string,
  ): Promise<void> {
    this.exito.set('');
    this.error.set('');

    if (formulario.invalid) {
      formulario.markAllAsTouched();
      return;
    }

    this.ocupado.set(true);

    try {
      this.exito.set(alExito(await firstValueFrom(accion())));
    } catch (fallo) {
      this.error.set(this.mensajeDeError(fallo));
    } finally {
      this.ocupado.set(false);
    }
  }

  private mensajeDeError(fallo: unknown): string {
    if (fallo instanceof HttpErrorResponse) {
      const mensaje = fallo.error?.mensaje ?? fallo.error?.message;

      if (typeof mensaje === 'string' && mensaje) {
        return mensaje;
      }

      if (fallo.status === 403) {
        return 'No tienes permiso para administrar RabbitMQ. Se requiere el rol ADMIN.';
      }
    }

    return 'No pudimos completar la operación. Revisa tu conexión e intenta nuevamente.';
  }
}
