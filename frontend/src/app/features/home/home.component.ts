import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BffService, PerfilResponse } from '../../core/services/bff.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.component.html',
})
export class HomeComponent {
  private readonly auth = inject(AuthService);
  private readonly bff = inject(BffService);

  protected readonly profile = signal<PerfilResponse | null>(null);
  protected readonly loadingProfile = signal(false);
  protected readonly profileError = signal('');

  protected readonly routeTiles = [
    {
      label: 'Pedidos',
      path: '/pedidos',
      description: 'Crear pedido, revisar seguimiento y leer el tracking consolidado.',
    },
    {
      label: 'Envios',
      path: '/envios',
      description: 'Ver envíos asignados y cambiar su estado según la operación.',
    },
    {
      label: 'Admin',
      path: '/admin',
      description: 'Gestionar usuarios, pedidos y crear envíos desde el panel central.',
    },
  ];

  protected readonly highlights = [
    { title: 'BFF', value: '8080', detail: 'Un solo origen para Angular' },
    { title: 'Roles', value: '3', detail: 'ADMIN, CLIENTE, REPARTIDOR' },
    { title: 'Flujo', value: 'Central', detail: 'Despacho y trazabilidad' },
  ];

  protected readonly roleCards = [
    {
      title: 'CLIENTE',
      text: 'Crea pedidos, revisa seguimiento y consume solo endpoints preparados para su rol.',
    },
    {
      title: 'REPARTIDOR',
      text: 'Actualiza estados de envíos y consulta su cola de trabajo desde una interfaz clara.',
    },
    {
      title: 'ADMIN',
      text: 'Administra usuarios, pedidos y envíos con acceso completo al panel de control.',
    },
  ];

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated()) {
        void this.loadProfile();
      } else {
        this.profile.set(null);
        this.profileError.set('');
      }
    });
  }

  protected readonly apiChecklist = [
    'Login contra Azure AD con MSAL.',
    'Bearer token adjunto automáticamente al BFF.',
    'Roles leídos desde `roles` para mostrar el menú.',
    'Respuesta del perfil desde `/bff/me` al entrar.',
  ];

  async loadProfile(): Promise<void> {
    if (this.loadingProfile()) {
      return;
    }

    this.loadingProfile.set(true);
    this.profileError.set('');

    try {
      const profile = await firstValueFrom(this.bff.me());
      this.profile.set(profile);
    } catch {
      this.profileError.set('No se pudo leer el perfil desde /bff/me. Verifica sesión y permisos.');
    } finally {
      this.loadingProfile.set(false);
    }
  }
}