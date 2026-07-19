import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Every screen in this dashboard is admin-only, so one guard covers the
 * whole app rather than separate "authenticated" vs "admin" guards.
 * currentUser is refetched here (not just read from memory) because a page
 * refresh loses in-memory state -- the httpOnly cookie is the only real
 * source of truth for whether the session is still valid.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.fetchCurrentUser().pipe(
    map((user) => {
      if (user?.role === 'ADMIN') {
        return true;
      }
      return router.createUrlTree(['/login']);
    }),
  );
};
