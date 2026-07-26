import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'browse',
        canActivate: [roleGuard],
        data: { roles: ['TRAVELER'] },
        loadComponent: () =>
          import('./features/browse/browse.component').then((m) => m.BrowseComponent),
      },
      {
        path: 'my-trips',
        canActivate: [roleGuard],
        data: { roles: ['TRAVELER'] },
        loadComponent: () =>
          import('./features/my-trips/my-trips.component').then((m) => m.MyTripsComponent),
      },
      {
        path: 'travels',
        canActivate: [roleGuard],
        data: { roles: ['TRAVEL_MANAGER', 'ADMIN'] },
        loadComponent: () =>
          import('./features/travels/travels-list.component').then((m) => m.TravelsListComponent),
      },
      {
        path: 'users',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./features/users/users-list.component').then((m) => m.UsersListComponent),
      },
      {
        path: 'reports',
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/reports/reports.component').then((m) => m.ReportsComponent),
      },
      {
        path: 'managers/:id',
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/managers/manager-detail.component').then((m) => m.ManagerDetailComponent),
      },
      {
        path: 'payments',
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/payments/payments-list.component').then((m) => m.PaymentsListComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
