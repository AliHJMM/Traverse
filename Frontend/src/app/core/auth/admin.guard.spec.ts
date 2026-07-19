import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { CanActivateFn } from '@angular/router';

import { adminGuard } from './admin.guard';

describe('adminGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => adminGuard(...guardParameters));

  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('allows access for an ADMIN user', (done) => {
    const result = executeGuard({} as never, {} as never);
    (result as any).subscribe((allowed: boolean | UrlTree) => {
      expect(allowed).toBe(true);
      done();
    });

    httpMock.expectOne('/api/auth/me').flush({ id: 1, email: 'admin@example.com', role: 'ADMIN' });
  });

  it('redirects to /login for a non-ADMIN user', (done) => {
    const router = TestBed.inject(Router);
    const result = executeGuard({} as never, {} as never);
    (result as any).subscribe((decision: boolean | UrlTree) => {
      expect(router.serializeUrl(decision as UrlTree)).toBe('/login');
      done();
    });

    httpMock.expectOne('/api/auth/me').flush({ id: 2, email: 'user@example.com', role: 'USER' });
  });

  it('redirects to /login when unauthenticated', (done) => {
    const router = TestBed.inject(Router);
    const result = executeGuard({} as never, {} as never);
    (result as any).subscribe((decision: boolean | UrlTree) => {
      expect(router.serializeUrl(decision as UrlTree)).toBe('/login');
      done();
    });

    httpMock.expectOne('/api/auth/me').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });
});
