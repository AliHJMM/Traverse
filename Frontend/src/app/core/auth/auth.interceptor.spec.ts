import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  it('attaches withCredentials to every request', () => {
    httpClient.get('/api/users').subscribe();
    const req = httpMock.expectOne('/api/users');
    expect(req.request.withCredentials).toBeTrue();
    req.flush([]);
  });

  it('redirects to /login on a 401 from a non-auth endpoint', () => {
    const navigateSpy = spyOn(router, 'navigate');

    httpClient.get('/api/users').subscribe({ error: () => {} });
    httpMock.expectOne('/api/users').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('does not redirect on a 401 from the login endpoint itself', () => {
    const navigateSpy = spyOn(router, 'navigate');

    httpClient.post('/api/auth/login', {}).subscribe({ error: () => {} });
    httpMock.expectOne('/api/auth/login').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
