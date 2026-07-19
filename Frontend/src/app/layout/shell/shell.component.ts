import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { filter, map, startWith } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';

const SECTION_TITLES: Record<string, string> = {
  '/users': 'Users',
  '/travels': 'Travels',
  '/payments': 'Payments',
};

@Component({
  selector: 'app-shell',
  imports: [
    AsyncPipe,
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

  readonly currentUser$ = this.authService.currentUser$;

  readonly sectionTitle = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => SECTION_TITLES[event.urlAfterRedirects] ?? ''),
      startWith(SECTION_TITLES[this.router.url] ?? ''),
    ),
    { initialValue: '' },
  );

  logout(): void {
    this.authService.logout().subscribe(() => this.router.navigateByUrl('/login'));
  }
}
