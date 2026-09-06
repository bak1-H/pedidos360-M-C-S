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
  readonly refreshState = signal<SessionState>('idle');
  readonly lastMessage = signal<string>('Ready to authenticate with Azure AD.');

  readonly isAuthenticated = computed(() => this.activeAccount() !== null);
  readonly displayName = computed(() => this.resolveDisplayName(this.activeAccount()));
  readonly email = computed(() => this.resolveEmail(this.activeAccount()));
  readonly roles = computed(() => this.resolveRoles(this.activeAccount()));

  constructor() {
    this.broadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.syncActiveAccount());
  }

  async initialize(): Promise<void> {
    await this.msalService.instance.initialize();

    this.msalService.handleRedirectObservable().subscribe({
      next: (result) => {
        if (result?.account) {
          this.msalService.instance.setActiveAccount(result.account);
        }

        this.syncActiveAccount();
      },
      error: () => this.syncActiveAccount(),
      complete: () => this.syncActiveAccount(),
    });

    this.syncActiveAccount();
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

      this.setAuthenticatedAccount(result);
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
      await this.loginPopup();
      return;
    }

    this.refreshState.set('refreshing');

    try {
      const result = await firstValueFrom(this.msalService.acquireTokenSilent({
        account,
        scopes: appSettings.loginScopes,
      }));

      this.setAuthenticatedAccount(result);
      this.refreshState.set('ready');
      this.lastMessage.set('Access token renewed silently.');
    } catch {
      this.refreshState.set('error');
      this.lastMessage.set('Silent refresh failed. Reauthentication required.');
      return;
    }
  }

  private syncActiveAccount(): void {
    const selectedAccount = this.msalService.instance.getActiveAccount() ?? this.msalService.instance.getAllAccounts()[0] ?? null;

    if (selectedAccount) {
      this.msalService.instance.setActiveAccount(selectedAccount);
    }

    this.activeAccount.set(selectedAccount);
  }

  private setAuthenticatedAccount(result: AuthenticationResult): void {
    const account = result.account ?? null;

    if (account) {
      this.msalService.instance.setActiveAccount(account);
    }

    this.activeAccount.set(account);
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
}