import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { appSettings, NavigationItem } from './core/config/app-settings';

@Component({
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly appName = appSettings.appName;
  protected readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly ahora = signal(new Date());

  protected readonly hora = computed(() =>
    this.ahora().toLocaleTimeString('es-CL', { hour12: false }),
  );

  protected readonly fecha = computed(() =>
    this.ahora().toLocaleDateString('es-CL', { weekday: 'short', day: 'numeric', month: 'short' }),
  );

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Inicio', path: '/', roles: [], exact: true },
    { label: 'Pedidos', path: '/pedidos', roles: ['CLIENTE', 'ADMIN'] },
    { label: 'Envios', path: '/envios', roles: ['REPARTIDOR'] },
    { label: 'Administracion', path: '/admin', roles: ['ADMIN'] },
  ];

  protected readonly visibleNavigation = computed(() =>
    this.navigationItems.filter((item) => this.canSee(item)),
  );

  constructor() {
    const tick = setInterval(() => this.ahora.set(new Date()), 1000);
    this.destroyRef.onDestroy(() => clearInterval(tick));
  }

  protected logout(): void {
    this.auth.logout();
  }

  private canSee(item: NavigationItem): boolean {
    if (item.roles.length === 0) {
      return true;
    }

    return this.auth.isAuthenticated() && this.auth.hasAnyRole(item.roles);
  }
}
