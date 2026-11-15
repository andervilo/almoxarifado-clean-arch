import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layouts/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login-page/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'acesso-negado',
    loadComponent: () =>
      import('./features/auth/access-denied/access-denied.component').then(
        (m) => m.AccessDeniedComponent,
      ),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        canActivate: [roleGuard(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'])],
        loadComponent: () =>
          import('./features/dashboard/dashboard-page/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
      },
      {
        path: 'catalogo',
        canActivate: [roleGuard(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'])],
        loadComponent: () =>
          import('./features/catalogo/catalogo-page/catalogo-page.component').then(
            (m) => m.CatalogoPageComponent,
          ),
      },
      {
        path: 'estoque',
        canActivate: [roleGuard(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'])],
        loadComponent: () =>
          import('./features/estoque/estoque-page/estoque-page.component').then(
            (m) => m.EstoquePageComponent,
          ),
      },
      {
        path: 'requisicoes',
        canActivate: [
          roleGuard([
            'ADMIN_PLATAFORMA',
            'ADMIN_ORGANIZACAO',
            'ALMOXARIFE',
            'REQUISITANTE',
            'GESTOR',
          ]),
        ],
        loadComponent: () =>
          import('./features/requisicoes/requisicoes-page/requisicoes-page.component').then(
            (m) => m.RequisicoesPageComponent,
          ),
      },
      {
        path: 'inventario',
        canActivate: [roleGuard(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'])],
        loadComponent: () =>
          import('./features/inventario/inventarios-page/inventarios-page.component').then(
            (m) => m.InventariosPageComponent,
          ),
      },
      {
        path: 'relatorios',
        canActivate: [roleGuard(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'])],
        loadComponent: () =>
          import('./features/relatorios/relatorios-page/relatorios-page.component').then(
            (m) => m.RelatoriosPageComponent,
          ),
      },
      {
        path: 'configuracoes',
        canActivate: [roleGuard(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'])],
        loadComponent: () =>
          import('./features/configuracoes/configuracoes-page/configuracoes-page.component').then(
            (m) => m.ConfiguracoesPageComponent,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
