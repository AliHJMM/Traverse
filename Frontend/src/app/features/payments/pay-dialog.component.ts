import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { PaymentMethod } from '../../core/models/payment.model';
import { PaymentService } from '../../core/services/payment.service';

export interface PayDialogData {
  travelId: number;
  travelTitle: string;
  amount: number;
}

@Component({
  selector: 'app-pay-dialog',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    RouterLink,
  ],
  templateUrl: './pay-dialog.component.html',
})
export class PayDialogComponent {
  private readonly paymentService = inject(PaymentService);
  private readonly dialogRef = inject(MatDialogRef<PayDialogComponent>);
  readonly data = inject<PayDialogData>(MAT_DIALOG_DATA);

  readonly methods = signal<PaymentMethod[]>([]);
  readonly loading = signal(true);
  readonly paying = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly methodControl = new FormControl<number | null>(null, { validators: [Validators.required] });

  constructor() {
    this.paymentService.findAll().subscribe({
      next: (methods) => {
        this.methods.set(methods);
        const preferred = methods.find((m) => m.isDefault) ?? methods[0];
        if (preferred) {
          this.methodControl.setValue(preferred.id);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Failed to load your payment methods.');
      },
    });
  }

  label(method: PaymentMethod): string {
    if (method.provider === 'STRIPE') {
      return `${method.brand ?? 'Card'} •••• ${method.last4}`;
    }
    return `PayPal ${method.payerEmail ?? ''}`.trim();
  }

  pay(): void {
    if (this.methodControl.invalid) {
      this.methodControl.markAsTouched();
      return;
    }
    this.paying.set(true);
    this.errorMessage.set(null);
    this.paymentService
      .charge({ travelId: this.data.travelId, paymentMethodId: this.methodControl.value! })
      .subscribe({
        next: (payment) => {
          this.paying.set(false);
          this.dialogRef.close(payment);
        },
        error: (err) => {
          this.paying.set(false);
          this.errorMessage.set(
            err?.status === 402 ? 'The payment was declined. Try another method.' : 'Payment failed. Please try again.',
          );
        },
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
