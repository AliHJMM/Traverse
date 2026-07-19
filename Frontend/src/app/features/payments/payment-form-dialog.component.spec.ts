import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { PaymentMethod } from '../../core/models/payment.model';
import { StripeLoaderService } from '../../core/services/stripe-loader.service';
import { PaymentFormDialogComponent, PaymentFormDialogData } from './payment-form-dialog.component';

describe('PaymentFormDialogComponent', () => {
  let component: PaymentFormDialogComponent;
  let fixture: ComponentFixture<PaymentFormDialogComponent>;
  let httpMock: HttpTestingController;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<PaymentFormDialogComponent>>;
  let cardElementMock: jasmine.SpyObj<{ mount: () => void; on: () => void; destroy: () => void }>;
  let stripeMock: { elements: () => unknown; createPaymentMethod: jasmine.Spy };

  const paymentMethod: PaymentMethod = {
    id: 1,
    userId: 42,
    provider: 'STRIPE',
    brand: 'visa',
    last4: '4242',
    expiryMonth: 12,
    expiryYear: 2030,
    payerEmail: null,
    isDefault: false,
    createdAt: '2026-01-01T00:00:00Z',
  };

  async function setup(data: PaymentFormDialogData) {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    cardElementMock = jasmine.createSpyObj('StripeCardElement', ['mount', 'on', 'destroy']);
    stripeMock = {
      elements: () => ({ create: () => cardElementMock }),
      createPaymentMethod: jasmine
        .createSpy('createPaymentMethod')
        .and.resolveTo({ paymentMethod: { id: 'pm_test_123' }, error: undefined }),
    };
    const stripeLoaderSpy = jasmine.createSpyObj('StripeLoaderService', ['load']);
    stripeLoaderSpy.load.and.resolveTo(stripeMock);

    await TestBed.configureTestingModule({
      imports: [PaymentFormDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: StripeLoaderService, useValue: stripeLoaderSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentFormDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    await fixture.whenStable();
  }

  afterEach(() => httpMock.verify());

  it('mounts the Stripe card element on init when provider is STRIPE', async () => {
    await setup({});
    expect(cardElementMock.mount).toHaveBeenCalled();
  });

  it('prefills userId when provided via dialog data', async () => {
    await setup({ userId: 42 });
    expect(component.form.controls.userId.value).toBe(42);
  });

  it('creates a Stripe payment method token and posts it to /api/payments', async () => {
    await setup({ userId: 42 });

    await component.submit();

    expect(stripeMock.createPaymentMethod).toHaveBeenCalled();
    const req = httpMock.expectOne({ method: 'POST', url: '/api/payments' });
    expect(req.request.body).toEqual({ userId: 42, provider: 'STRIPE', token: 'pm_test_123', setDefault: false });
    req.flush(paymentMethod);

    expect(dialogRefSpy.close).toHaveBeenCalledWith(paymentMethod);
  });

  it('shows a card error and does not submit when Stripe declines the card', async () => {
    await setup({ userId: 42 });
    stripeMock.createPaymentMethod.and.resolveTo({ error: { message: 'Your card was declined.' } });

    await component.submit();

    expect(component.cardError()).toBe('Your card was declined.');
    httpMock.expectNone('/api/payments');
  });

  it('requires a PayPal token when the PayPal provider is selected', async () => {
    await setup({ userId: 42 });
    component.form.patchValue({ provider: 'PAYPAL' });
    component.onProviderChange();

    await component.submit();

    expect(component.errorMessage()).toBe('A PayPal payment token is required.');
    httpMock.expectNone('/api/payments');
  });

  it('submits a PayPal token directly without calling Stripe', async () => {
    await setup({ userId: 42 });
    component.form.patchValue({ provider: 'PAYPAL', paypalToken: 'paypal_token_abc' });
    component.onProviderChange();

    await component.submit();

    const req = httpMock.expectOne({ method: 'POST', url: '/api/payments' });
    expect(req.request.body).toEqual({
      userId: 42,
      provider: 'PAYPAL',
      token: 'paypal_token_abc',
      setDefault: false,
    });
    req.flush({ ...paymentMethod, provider: 'PAYPAL' });

    expect(stripeMock.createPaymentMethod).not.toHaveBeenCalled();
  });

  it('cancel closes without a result', async () => {
    await setup({});
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });
});
