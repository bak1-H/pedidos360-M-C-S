import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BffService, PerfilResponse } from '../../core/services/bff.service';

interface AccesoDirecto {
  label: string;
  path: string;
  descripcion: string;
  roles: string[];
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.component.html',
})
export class HomeComponent {
  private readonly auth = inject(AuthService);
  private readonly bff = inject(BffService);

  protected readonly autenticado = this.auth.isAuthenticated;
  protected readonly roles = this.auth.roles;
  protected readonly profile = signal<PerfilResponse | null>(null);
  protected readonly loadingProfile = signal(false);
  protected readonly profileError = signal('');

  private readonly accesos: AccesoDirecto[] = [
    {
      label: 'Mis pedidos',
      path: '/pedidos',
      descripcion: 'Crea un pedido nuevo y sigue el estado de los que ya enviaste.',
      roles: ['CLIENTE', 'ADMIN'],
    },
    {
      label: 'Mis envios',
      path: '/envios',
      descripcion: 'Revisa las entregas asignadas y actualiza su estado en ruta.',
      roles: ['REPARTIDOR'],
    },
    {
      label: 'Administracion',
      path: '/admin',
      descripcion: 'Asigna repartidores a los pedidos y supervisa la operacion.',
      roles: ['ADMIN'],
    },
  ];

  protected readonly accesosVisibles = computed(() =>
    this.accesos.filter((acceso) => this.auth.hasAnyRole(acceso.roles)),
  );

  constructor() {
    effect(() => {
      const authenticated = this.auth.isAuthenticated();

      untracked(() => {
        if (authenticated) {
          void this.loadProfile();
        } else {
          this.profile.set(null);
          this.profileError.set('');
        }
      });
    });
  }

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
      this.profileError.set('No pudimos cargar tu perfil. Vuelve a intentarlo en unos segundos.');
    } finally {
      this.loadingProfile.set(false);
    }
  }
}
