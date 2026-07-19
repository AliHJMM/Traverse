import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { PaymentMethod } from '../../core/models/payment.model';
import { PaymentsListComponent } from './payments-list.component';

describe('PaymentsListComponent', () => {
  let component: PaymentsListComponent;
  let fixture: ComponentFixture<PaymentsListComponent>;
  let httpMock: HttpTestingController;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const method: PaymentMethod = {
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

  beforeEach(async () => {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [PaymentsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentsListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpMock.expectOne({ method: 'GET', url: '/api/payments' }).flush([method]);
  });

  afterEach(() => httpMock.verify());

  it('loads payment methods on init', () => {
    expect(component.paymentMethods()).toEqual([method]);
  });

  it('formats Stripe card details', () => {
    expect(component.details(method)).toContain('4242');
  });

  it('formats PayPal details as the payer email', () => {
    expect(component.details({ ...method, provider: 'PAYPAL', payerEmail: 'buyer@example.com' })).toBe(
      'buyer@example.com',
    );
  });

  it('reloads with the userId filter applied', () => {
    component.userIdFilter = 42;
    component.reload();

    const req = httpMock.expectOne((r) => r.url === '/api/payments' && r.params.get('userId') === '42');
    req.flush([method]);

    expect(component.paymentMethods()).toEqual([method]);
  });

  it('reloads the list after a successful create dialog', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(method) } as ReturnType<MatDialog['open']>);

    component.openCreateDialog();
    httpMock.expectOne({ method: 'GET', url: '/api/payments' }).flush([method, { ...method, id: 2 }]);

    expect(component.paymentMethods().length).toBe(2);
  });

  it('deletes a payment method after confirmation', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as ReturnType<MatDialog['open']>);

    component.confirmDelete(method);
    httpMock.expectOne({ method: 'DELETE', url: '/api/payments/1' }).flush(null);
    httpMock.expectOne({ method: 'GET', url: '/api/payments' }).flush([]);

    expect(component.paymentMethods()).toEqual([]);
  });
});
