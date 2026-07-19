import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UserService } from './user.service';
import { User } from '../models/user.model';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const user: User = {
    id: 1,
    email: 'traveler@example.com',
    role: 'USER',
    fullName: 'Jane Traveler',
    phone: null,
    address: null,
    enabled: true,
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('findAll GETs /api/users', () => {
    service.findAll().subscribe((users) => expect(users).toEqual([user]));
    httpMock.expectOne({ method: 'GET', url: '/api/users' }).flush([user]);
  });

  it('findById GETs /api/users/:id', () => {
    service.findById(1).subscribe((result) => expect(result).toEqual(user));
    httpMock.expectOne({ method: 'GET', url: '/api/users/1' }).flush(user);
  });

  it('create POSTs to /api/users', () => {
    const request = { email: 'traveler@example.com', password: 'password123', role: 'USER' as const, fullName: 'Jane Traveler' };
    service.create(request).subscribe((result) => expect(result).toEqual(user));

    const req = httpMock.expectOne({ method: 'POST', url: '/api/users' });
    expect(req.request.body).toEqual(request);
    req.flush(user);
  });

  it('update PUTs to /api/users/:id', () => {
    service.update(1, { fullName: 'Jane T.' }).subscribe((result) => expect(result).toEqual(user));

    const req = httpMock.expectOne({ method: 'PUT', url: '/api/users/1' });
    expect(req.request.body).toEqual({ fullName: 'Jane T.' });
    req.flush(user);
  });

  it('delete DELETEs /api/users/:id', () => {
    service.delete(1).subscribe();
    httpMock.expectOne({ method: 'DELETE', url: '/api/users/1' }).flush(null);
  });
});
