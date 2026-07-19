import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';
import { CurrentUser } from '../models/current-user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const admin: CurrentUser = { id: 1, email: 'admin@example.com', role: 'ADMIN' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('login stores the returned user as the current user', () => {
    service.login('admin@example.com', 'password123').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(admin);

    expect(service.currentUser).toEqual(admin);
  });

  it('logout clears the current user', () => {
    service.login('admin@example.com', 'password123').subscribe();
    httpMock.expectOne('/api/auth/login').flush(admin);
    expect(service.currentUser).toEqual(admin);

    service.logout().subscribe();
    httpMock.expectOne('/api/auth/logout').flush(null);

    expect(service.currentUser).toBeNull();
  });

  it('fetchCurrentUser populates the current user on success', () => {
    service.fetchCurrentUser().subscribe();

    httpMock.expectOne('/api/auth/me').flush(admin);

    expect(service.currentUser).toEqual(admin);
  });

  it('fetchCurrentUser resolves to null (not an error) when unauthenticated', () => {
    let result: CurrentUser | null | undefined;

    service.fetchCurrentUser().subscribe((user) => (result = user));

    httpMock.expectOne('/api/auth/me').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(result).toBeNull();
    expect(service.currentUser).toBeNull();
  });
});
