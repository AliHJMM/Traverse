import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { PaymentMethod } from '../../core/models/payment.model';
import { PayDialogComponent } from './pay-dialog.component';

describe('PayDialogComponent', () => {
  let component: PayDialogComponent;
  let fixture: ComponentFixture<PayDialogComponent>;
  let httpMock: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<PayDialogComponent>>;

  const method: PaymentMethod = {
    id: 7,
    userId: 2,
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
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      imports: [PayDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { travelId: 1, travelTitle: 'Trip', amount: 500 } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PayDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('loads methods and preselects the default', () => {
    httpMock.expectOne('/api/payments').flush([method]);
    expect(component.methods().length).toBe(1);
    expect(component.methodControl.value).toBe(7);
  });

  it('charges the selected method and closes with the payment', () => {
    httpMock.expectOne('/api/payments').flush([method]);

    component.pay();
    const req = httpMock.expectOne('/api/payments/charges');
    expect(req.request.body).toEqual({ travelId: 1, paymentMethodId: 7, amount: 500 });
    req.flush({ id: 9, travelId: 1, status: 'SUCCEEDED' });

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('surfaces a declined charge (402) without closing', () => {
    httpMock.expectOne('/api/payments').flush([method]);

    component.pay();
    httpMock
      .expectOne('/api/payments/charges')
      .flush({ status: 'FAILED' }, { status: 402, statusText: 'Payment Required' });

    expect(component.errorMessage()).toContain('declined');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
