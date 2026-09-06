import { APP_INITIALIZER, ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { MsalBroadcastService, MsalGuard, MsalInterceptor, MsalService, MSAL_GUARD_CONFIG, MSAL_INTERCEPTOR_CONFIG, MSAL_INSTANCE } from '@azure/msal-angular';
import { AuthService } from './core/auth/auth.service';
import { msalGuardConfigFactory, msalInstanceFactory, msalInterceptorConfigFactory } from './core/auth/msal.factories';
import { routes } from './app.routes';

export function initializeAuthFactory(authService: AuthService): () => Promise<void> {
  return () => authService.initialize();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: APP_INITIALIZER, useFactory: initializeAuthFactory, deps: [AuthService], multi: true },
    { provide: MSAL_INSTANCE, useFactory: msalInstanceFactory },
    { provide: MSAL_GUARD_CONFIG, useFactory: msalGuardConfigFactory },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: msalInterceptorConfigFactory },
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
  ]
};
