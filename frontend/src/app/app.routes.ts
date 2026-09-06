import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
	{
		path: '',
		pathMatch: 'full',
		loadComponent: () => import('./features/home/home.component').then((module) => module.HomeComponent),
	},
	{
		path: 'pedidos',
		canActivate: [MsalGuard, roleGuard],
		data: { roles: ['CLIENTE', 'ADMIN'] },
		loadComponent: () => import('./features/pedidos/pedidos.component').then((module) => module.PedidosComponent),
	},
	{
		path: 'envios',
		canActivate: [MsalGuard, roleGuard],
		data: { roles: ['REPARTIDOR'] },
		loadComponent: () => import('./features/envios/envios.component').then((module) => module.EnviosComponent),
	},
	{
		path: 'admin',
		canActivate: [MsalGuard, roleGuard],
		data: { roles: ['ADMIN'] },
		loadComponent: () => import('./features/admin/admin.component').then((module) => module.AdminComponent),
	},
	{
		path: '**',
		redirectTo: '',
	},
];
