import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import type { Stripe, StripeCardElement, StripeElements } from '@stripe/stripe-js';

import { PaymentService } from '../../core/services/payment.service';
import { StripeLoaderService } from '../../core/services/stripe-loader.service';

export interface PaymentFormDialogData {
  userId?: number;
}

@Component({
  selector: 'app-payment-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  templateUrl: './payment-form-dialog.component.html',
})
export class PaymentFormDialogComponent implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly paymentService = inject(PaymentService);
  private readonly stripeLoader = inject(StripeLoaderService);
  private readonly dialogRef = inject(MatDialogRef<PaymentFormDialogComponent>);
  private readonly data = inject<PaymentFormDialogData>(MAT_DIALOG_DATA, { optional: true }) ?? {};

  @ViewChild('cardElement') private readonly cardElementContainer?: ElementRef<HTMLDivElement>;

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private cardElement: StripeCardElement | null = null;

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly cardError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    userId: [this.data.userId ?? (null as number | null), [Validators.required]],
    provider: ['STRIPE' as 'STRIPE' | 'PAYPAL', [Validators.required]],
    paypalToken: [''],
    setDefault: [false],
  });

  ngAfterViewInit(): void {
    void this.mountStripeCardElement();
  }

  ngOnDestroy(): void {
    this.cardElement?.destroy();
  }

  onProviderChange(): void {
    if (this.form.controls.provider.value === 'STRIPE') {
      void this.mountStripeCardElement();
    } else {
      this.cardElement?.destroy();
      this.cardElement = null;
    }
  }

  private async mountStripeCardElement(): Promise<void> {
    if (this.form.controls.provider.value !== 'STRIPE' || this.cardElement || !this.cardElementContainer) {
      return;
    }

    this.stripe = await this.stripeLoader.load();
    if (!this.stripe) {
      this.errorMessage.set('Failed to load Stripe.');
      return;
    }

    this.elements = this.stripe.elements();
    this.cardElement = this.elements.create('card');
    this.cardElement.mount(this.cardElementContainer.nativeElement);
    this.cardElement.on('change', (event) => {
      this.cardError.set(event.error?.message ?? null);
    });
  }

  async submit(): Promise<void> {
    if (this.form.controls.userId.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { userId, provider, paypalToken, setDefault } = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);

    let token: string;
    if (provider === 'STRIPE') {
      const tokenResult = await this.createStripeToken();
      if (!tokenResult) {
        this.saving.set(false);
        return;
      }
      token = tokenResult;
    } else {
      if (!paypalToken) {
        this.errorMessage.set('A PayPal payment token is required.');
        this.saving.set(false);
        return;
      }
      token = paypalToken;
    }

    this.paymentService.create({ userId: userId!, provider, token, setDefault }).subscribe({
      next: (paymentMethod) => {
        this.saving.set(false);
        this.dialogRef.close(paymentMethod);
      },
      error: (error) => {
        this.saving.set(false);
        this.errorMessage.set(error?.error?.error ?? 'Something went wrong. Please try again.');
      },
    });
  }

  private async createStripeToken(): Promise<string | null> {
    if (!this.stripe || !this.cardElement) {
      this.errorMessage.set('Card details are not ready yet.');
      return null;
    }

    const result = await this.stripe.createPaymentMethod({ type: 'card', card: this.cardElement });
    if (result.error) {
      this.cardError.set(result.error.message ?? 'Card declined.');
      return null;
    }
    return result.paymentMethod.id;
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
