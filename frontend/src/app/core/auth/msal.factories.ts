import { InteractionType, IPublicClientApplication, PublicClientApplication, BrowserCacheLocation } from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import { appSettings } from '../config/app-settings';

export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: appSettings.clientId,
      authority: `https://login.microsoftonline.com/${appSettings.tenantId}`,
      redirectUri: appSettings.redirectUri,
      postLogoutRedirectUri: appSettings.postLogoutRedirectUri,
      navigateToLoginRequestUrl: false,
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage,
      storeAuthStateInCookie: false,
    },
  });
}

export function msalGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: appSettings.loginScopes,
      redirectUri: appSettings.redirectUri,
    },
  };
}

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>();
  protectedResourceMap.set(appSettings.bffApiUrl, appSettings.loginScopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}