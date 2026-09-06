export interface NavigationItem {
  label: string;
  path: string;
  roles: string[];
  exact?: boolean;
}

export const appSettings = {
  appName: 'Pedidos360',
  tenantId: 'a0937f8c-6713-4912-8198-29b1db9da9a5',
  clientId: '6a91057e-d0bd-4762-8b34-e63e6ef931e0',
  apiClientId: '598d6a64-0770-4213-bb07-34601e54499d',
  redirectUri: 'http://localhost:4200',
  postLogoutRedirectUri: 'http://localhost:4200',
  bffApiUrl: 'http://localhost:8080/bff',
  loginScopes: ['api://598d6a64-0770-4213-bb07-34601e54499d/access_as_user'],
  featureRoles: ['ADMIN', 'REPARTIDOR', 'CLIENTE'] as const,
};
