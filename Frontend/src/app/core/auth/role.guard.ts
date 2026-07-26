import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { Role } from '../models/current-user.model';
import { AuthService } from './auth.service';

/**
 * Gate for the authenticated shell. currentUser is refetched here (not just
 * read from memory) because a page refresh loses in-memory state -- the
 * httpOnly cookie is the only real source of truth for whether the session is
 * still valid. Child routes then read the (now-populated) currentUser
 * synchronously via {@link roleGuard}.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.fetchCurrentUser().pipe(
    map((user) => (user ? true : router.createUrlTree(['/login']))),
  );
};

/**
 * Per-route role gate. Reads {@code data.roles} on the route and checks it
 * against the current user (already loaded by {@link authGuard} on the parent
 * shell route). An empty/missing roles list means "any authenticated user".
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const allowed = (route.data['roles'] as Role[] | undefined) ?? [];
  const user = authService.currentUser;

  if (!user) {
    return router.createUrlTree(['/login']);
  }
  if (allowed.length === 0 || allowed.includes(user.role)) {
    return true;
  }
  return router.createUrlTree(['/dashboard']);
};
