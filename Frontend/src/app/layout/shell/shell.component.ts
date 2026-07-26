import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { filter, map, startWith } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/models/current-user.model';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', label: 'Dashboard', icon: 'dashboard', roles: ['ADMIN', 'TRAVEL_MANAGER', 'TRAVELER'] },
  { path: '/browse', label: 'Browse', icon: 'travel_explore', roles: ['TRAVELER'] },
  { path: '/my-trips', label: 'My Trips', icon: 'luggage', roles: ['TRAVELER'] },
  { path: '/travels', label: 'Travels', icon: 'flight', roles: ['ADMIN', 'TRAVEL_MANAGER'] },
  { path: '/users', label: 'Users', icon: 'group', roles: ['ADMIN'] },
  { path: '/reports', label: 'Reports', icon: 'flag', roles: ['ADMIN', 'TRAVEL_MANAGER', 'TRAVELER'] },
  { path: '/payments', label: 'Payments', icon: 'payment', roles: ['ADMIN'] },
];

const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  TRAVEL_MANAGER: 'Travel Manager',
  TRAVELER: 'Traveler',
};

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatSidenavModule,
    MatToolbarModule,
    MatButtonModule,
  ],
  templateUrl: './shell.component.html',
})
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = toSignal(this.authService.currentUser$, { initialValue: null });

  readonly navItems = computed<NavItem[]>(() => {
    const role = this.currentUser()?.role;
    if (!role) {
      return [];
    }
    return NAV_ITEMS.filter((item) => item.roles.includes(role));
  });

  readonly roleLabel = computed<string>(() => {
    const role = this.currentUser()?.role;
    return role ? ROLE_LABELS[role] : '';
  });

  readonly sectionTitle = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => this.titleFor(event.urlAfterRedirects)),
      startWith(this.titleFor(this.router.url)),
    ),
    { initialValue: '' },
  );

  private titleFor(url: string): string {
    return NAV_ITEMS.find((item) => url.startsWith(item.path))?.label ?? '';
  }

  logout(): void {
    this.authService.logout().subscribe(() => this.router.navigateByUrl('/login'));
  }
}
