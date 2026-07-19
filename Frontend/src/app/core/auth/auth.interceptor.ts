import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * withCredentials is defensive: same-origin requests already send cookies
 * without it, but this keeps the app correct if it's ever proxied
 * differently. A 401 from any API call (except the auth endpoints
 * themselves, to avoid redirect loops on a failed login attempt) means the
 * session cookie is missing or expired -- bounce to the login screen.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const withCreds = req.clone({ withCredentials: true });

  return next(withCreds).pipe(
    catchError((error) => {
      const isAuthEndpoint = req.url.includes('/api/auth/');
      if (error?.status === 401 && !isAuthEndpoint) {
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
