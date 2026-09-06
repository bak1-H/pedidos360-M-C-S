import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AccountInfo, AuthenticationResult, InteractionStatus } from '@azure/msal-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom, filter } from 'rxjs';
import { appSettings } from '../config/app-settings';

type SessionState = 'idle' | 'refreshing' | 'ready' | 'error';

interface ClaimsWithRoles {
  name?: string;
  preferred_username?: string;
  email?: string;
  roles?: string[];
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly msalService = inject(MsalService);
  private readonly broadcastService = inject(MsalBroadcastService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly activeAccount = signal<AccountInfo | null>(null);
  private readonly accessTokenRoles = signal<string[] | null>(null);

  /**
   * homeAccountId del usuario para el que ya se pidieron los roles. Evita
   * repetir acquireTokenSilent en cada resincronizacion de cuenta: sin esto,
   * cualquier trigger reactivo (inProgress$, interceptor, etc.) termina
   * pidiendo el token de nuevo, lo que genera mas eventos de MSAL, lo que
   * dispara mas resincronizaciones, en un ciclo que en la practica se
   * convierte en miles de requests por minuto.
   */
  private rolesLoadedForAccount: string | null = null;

  readonly refreshState = signal<SessionState>('idle');
  readonly lastMessage = signal<string>('Ready to authenticate with Azure AD.');

  readonly isAuthenticated = computed(() => this.activeAccount() !== null);
  readonly displayName = computed(() => this.resolveDisplayName(this.activeAccount()));
  readonly email = computed(() => this.resolveEmail(this.activeAccount()));

  /**
   * Los App Roles estan definidos y asignados en el App Registration de la API
   * (pedidos360-api), no en el del SPA. Por eso NO aparecen en el idToken (emitido
   * para el SPA) sino en el accessToken pedido con el scope de la API — el mismo
   * que MsalInterceptor adjunta en cada llamada al BFF.
   */
  readonly roles = computed(() => this.accessTokenRoles() ?? this.resolveRoles(this.activeAccount()));

  constructor() {
    /**
     * Esta suscripcion SOLO mantiene sincronizados nombre/email para la UI.
     * A proposito NO dispara ninguna llamada de red (ver loadRolesOnce): con
     * MsalInterceptor pidiendo tokens en cada request al BFF, inProgress$
     * emite constantemente, y cualquier efecto secundario aca se amplifica.
     */
    this.broadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.syncActiveAccount());
  }

  async initialize(): Promise<void> {
    await this.msalService.instance.initialize();

    try {
      const redirectResult = await firstValueFrom(this.msalService.handleRedirectObservable());
      if (redirectResult) {
        this.applyAuthResult(redirectResult);
        return;
      }
    } catch {
      this.lastMessage.set('No se pudo completar el login por redireccion.');
    }

    this.syncActiveAccount();

    const account = this.activeAccount();
    if (account) {
      await this.loadRolesOnce(account);
    }
  }

  hasAnyRole(requiredRoles: readonly string[]): boolean {
    const currentRoles = this.roles();
    return requiredRoles.length === 0 || requiredRoles.some((role) => currentRoles.includes(role));
  }

  async loginPopup(): Promise<void> {
    try {
      const result = await firstValueFrom(this.msalService.loginPopup({
        scopes: appSettings.loginScopes,
      }));

      this.applyAuthResult(result);
      this.refreshState.set('ready');
      this.lastMessage.set('Authentication completed with popup.');
    } catch {
      this.refreshState.set('error');
      this.lastMessage.set('Popup login failed. Falling back to redirect.');
      this.loginRedirect();
    }
  }

  loginRedirect(): void {
    this.msalService.loginRedirect({
      scopes: appSettings.loginScopes,
      redirectUri: appSettings.redirectUri,
    });
  }

  logout(): void {
    this.msalService.logoutRedirect({
      postLogoutRedirectUri: appSettings.postLogoutRedirectUri,
    });
  }

  async refreshSession(): Promise<void> {
    const account = this.activeAccount();

    if (!account) {
      this.loginRedirect();
      return;
    }

    this.refreshState.set('refreshing');

    try {
      const result = await firstValueFrom(this.msalService.acquireTokenSilent({
        account,
        scopes: appSettings.loginScopes,
      }));

      this.applyAuthResult(result);
      this.refreshState.set('ready');
      this.lastMessage.set('Access token renewed silently.');
    } catch {
      this.refreshState.set('error');
      this.lastMessage.set('Silent refresh failed. Reauthentication required.');
    }
  }

  private syncActiveAccount(): void {
    const selectedAccount = this.msalService.instance.getActiveAccount() ?? this.msalService.instance.getAllAccounts()[0] ?? null;

    if (selectedAccount) {
      this.msalService.instance.setActiveAccount(selectedAccount);
    }

    this.updateActiveAccount(selectedAccount);
  }

  /**
   * MSAL devuelve una instancia de AccountInfo nueva en cada llamada, incluso
   * para la misma cuenta. Las signals de Angular comparan por referencia, asi
   * que comparamos por homeAccountId para no generar una emision — y por lo
   * tanto un retrigger de quien dependa de esta signal — cuando la cuenta
   * logica no cambio realmente.
   */
  private updateActiveAccount(account: AccountInfo | null): void {
    const current = this.activeAccount();
    if (current?.homeAccountId === account?.homeAccountId) {
      return;
    }

    this.activeAccount.set(account);
  }

  /** Login/redirect/refresh ya traen un accessToken valido para loginScopes: se decodifica directo, sin pedir otro. */
  private applyAuthResult(result: AuthenticationResult): void {
    const account = result.account ?? null;

    if (account) {
      this.msalService.instance.setActiveAccount(account);
    }

    this.updateActiveAccount(account);
    this.accessTokenRoles.set(this.decodeRolesFromAccessToken(result.accessToken));

    if (account) {
      this.rolesLoadedForAccount = account.homeAccountId;
    }
  }

  /** Unico caso que necesita un acquireTokenSilent aparte: sesion recuperada del storage al recargar la pagina. */
  private async loadRolesOnce(account: AccountInfo): Promise<void> {
    if (this.rolesLoadedForAccount === account.homeAccountId) {
      return;
    }

    try {
      const result = await firstValueFrom(this.msalService.acquireTokenSilent({
        account,
        scopes: appSettings.loginScopes,
      }));

      this.accessTokenRoles.set(this.decodeRolesFromAccessToken(result.accessToken));
      this.rolesLoadedForAccount = account.homeAccountId;
    } catch {
      this.accessTokenRoles.set([]);
    }
  }

  private resolveDisplayName(account: AccountInfo | null): string {
    if (!account) {
      return 'Sesion no iniciada';
    }

    const claims = account.idTokenClaims as ClaimsWithRoles | undefined;
    return claims?.name ?? account.name ?? account.username ?? 'Usuario autenticado';
  }

  private resolveEmail(account: AccountInfo | null): string {
    if (!account) {
      return 'Conecta tu cuenta de Azure AD';
    }

    const claims = account.idTokenClaims as ClaimsWithRoles | undefined;
    return claims?.preferred_username ?? claims?.email ?? account.username ?? 'Sin correo disponible';
  }

  private resolveRoles(account: AccountInfo | null): string[] {
    if (!account) {
      return [];
    }

    const claims = account.idTokenClaims as ClaimsWithRoles | undefined;
    return Array.isArray(claims?.roles) ? claims.roles : [];
  }

  private decodeRolesFromAccessToken(accessToken: string): string[] {
    try {
      const payload = accessToken.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const claims = JSON.parse(atob(normalized)) as ClaimsWithRoles;
      return Array.isArray(claims.roles) ? claims.roles : [];
    } catch {
      return [];
    }
  }
}
