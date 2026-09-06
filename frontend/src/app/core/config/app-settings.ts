export interface NavigationItem {
  label: string;
  path: string;
  roles: string[];
  exact?: boolean;
}

export const appSettings = {
  appName: 'Pedidos360',
  tenantId: 'REPLACE_WITH_TENANT_ID',
  clientId: 'REPLACE_WITH_SPA_CLIENT_ID',
  apiClientId: 'REPLACE_WITH_BFF_CLIENT_ID',
  redirectUri: 'http://localhost:4200',
  postLogoutRedirectUri: 'http://localhost:4200',
  bffApiUrl: 'http://localhost:8080/bff',
  loginScopes: ['api://REPLACE_WITH_BFF_CLIENT_ID/access_as_user'],
  featureRoles: ['ADMIN', 'REPARTIDOR', 'CLIENTE'] as const,
};
