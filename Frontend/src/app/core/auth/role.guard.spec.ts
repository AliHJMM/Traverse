import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, of } from 'rxjs';

import { CurrentUser } from '../models/current-user.model';
import { AuthService } from './auth.service';
import { authGuard, roleGuard } from './role.guard';

class AuthServiceStub {
  currentUser: CurrentUser | null = null;
  fetched: CurrentUser | null = null;
  fetchCurrentUser(): Observable<CurrentUser | null> {
    return of(this.fetched);
  }
}

describe('role guards', () => {
  let auth: AuthServiceStub;

  const run = (guard: CanActivateFn, route: Partial<ActivatedRouteSnapshot> = {}) =>
    TestBed.runInInjectionContext(() =>
      guard(route as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    auth = new AuthServiceStub();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: auth },
      ],
    });
  });

  it('authGuard allows an authenticated user', (done) => {
    auth.fetched = { id: 1, email: 'a@b.c', role: 'ADMIN' };
    (run(authGuard) as Observable<boolean | UrlTree>).subscribe((result) => {
      expect(result).toBeTrue();
      done();
    });
  });

  it('authGuard redirects to /login when unauthenticated', (done) => {
    auth.fetched = null;
    (run(authGuard) as Observable<boolean | UrlTree>).subscribe((result) => {
      expect(result instanceof UrlTree).toBeTrue();
      expect((result as UrlTree).toString()).toBe('/login');
      done();
    });
  });

  it('roleGuard allows a matching role', () => {
    auth.currentUser = { id: 1, email: 'm@b.c', role: 'TRAVEL_MANAGER' };
    expect(run(roleGuard, { data: { roles: ['TRAVEL_MANAGER', 'ADMIN'] } })).toBeTrue();
  });

  it('roleGuard allows any authenticated user when no roles are set', () => {
    auth.currentUser = { id: 2, email: 't@b.c', role: 'TRAVELER' };
    expect(run(roleGuard, { data: {} })).toBeTrue();
  });

  it('roleGuard redirects a mismatched role to /dashboard', () => {
    auth.currentUser = { id: 2, email: 't@b.c', role: 'TRAVELER' };
    const result = run(roleGuard, { data: { roles: ['ADMIN'] } });
    expect(result instanceof UrlTree).toBeTrue();
    expect((result as UrlTree).toString()).toBe('/dashboard');
  });

  it('roleGuard redirects to /login when there is no user', () => {
    auth.currentUser = null;
    const result = run(roleGuard, { data: {} });
    expect(result instanceof UrlTree).toBeTrue();
    expect((result as UrlTree).toString()).toBe('/login');
  });
});
