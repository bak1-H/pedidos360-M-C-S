import { Component, computed, inject } from '@angular/core';
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

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Inicio', path: '/', roles: [], exact: true },
    { label: 'Pedidos', path: '/pedidos', roles: ['CLIENTE', 'ADMIN'] },
    { label: 'Envios', path: '/envios', roles: ['REPARTIDOR', 'ADMIN'] },
    { label: 'Admin', path: '/admin', roles: ['ADMIN'] },
  ];

  protected readonly visibleNavigation = computed(() =>
    this.navigationItems.filter((item) => this.canSee(item)),
  );

  protected readonly roleHighlights = [
    {
      label: 'CLIENTE',
      detail: 'Crea pedidos y revisa el seguimiento desde el BFF.',
    },
    {
      label: 'REPARTIDOR',
      detail: 'Actualiza el estado de sus envios y ve su cola de trabajo.',
    },
    {
      label: 'ADMIN',
      detail: 'Gestiona usuarios, pedidos y envios desde un unico punto.',
    },
  ];

  protected loginPopup(): void {
    void this.auth.loginPopup();
  }

  protected loginRedirect(): void {
    this.auth.loginRedirect();
  }

  protected logout(): void {
    this.auth.logout();
  }

  protected refreshSession(): void {
    void this.auth.refreshSession();
  }

  private canSee(item: NavigationItem): boolean {
    if (item.roles.length === 0) {
      return true;
    }

    return this.auth.isAuthenticated() && this.auth.hasAnyRole(item.roles);
  }
}
