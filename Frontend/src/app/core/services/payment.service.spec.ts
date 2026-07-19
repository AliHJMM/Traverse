import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PaymentMethod } from '../models/payment.model';
import { PaymentService } from './payment.service';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  const paymentMethod: PaymentMethod = {
    id: 1,
    userId: 42,
    provider: 'STRIPE',
    brand: 'visa',
    last4: '4242',
    expiryMonth: 12,
    expiryYear: 2030,
    payerEmail: null,
    isDefault: true,
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('findAll GETs /api/payments without a userId filter', () => {
    service.findAll().subscribe((methods) => expect(methods).toEqual([paymentMethod]));
    httpMock.expectOne({ method: 'GET', url: '/api/payments' }).flush([paymentMethod]);
  });

  it('findAll GETs /api/payments?userId=... when filtering', () => {
    service.findAll(42).subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/payments' && r.params.get('userId') === '42');
    req.flush([paymentMethod]);
  });

  it('create POSTs to /api/payments', () => {
    const request = { userId: 42, provider: 'STRIPE' as const, token: 'pm_test_123', setDefault: true };
    service.create(request).subscribe((result) => expect(result).toEqual(paymentMethod));

    const req = httpMock.expectOne({ method: 'POST', url: '/api/payments' });
    expect(req.request.body).toEqual(request);
    req.flush(paymentMethod);
  });

  it('delete DELETEs /api/payments/:id', () => {
    service.delete(1).subscribe();
    httpMock.expectOne({ method: 'DELETE', url: '/api/payments/1' }).flush(null);
  });
});
