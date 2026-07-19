import { Routes } from '@angular/router';

import { adminGuard } from './core/auth/admin.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [adminGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'users' },
      {
        path: 'users',
        loadComponent: () => import('./features/users/users-list.component').then((m) => m.UsersListComponent),
      },
      {
        path: 'travels',
        loadComponent: () =>
          import('./features/travels/travels-list.component').then((m) => m.TravelsListComponent),
      },
      {
        path: 'payments',
        loadComponent: () =>
          import('./features/payments/payments-list.component').then((m) => m.PaymentsListComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'users' },
];
