import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { AuthService } from '../../core/auth/auth.service';
import { Payment, PaymentMethod } from '../../core/models/payment.model';
import { PaymentService } from '../../core/services/payment.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { PaymentFormDialogComponent } from './payment-form-dialog.component';

@Component({
  selector: 'app-payments-list',
  // MatDialogModule intentionally not imported -- see UsersListComponent
  // for why (it would shadow the app-wide MatDialog instance in tests).
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './payments-list.component.html',
})
export class PaymentsListComponent {
  private readonly paymentService = inject(PaymentService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly isAdmin = this.authService.currentUser?.role === 'ADMIN';
  readonly methodColumns = this.isAdmin
    ? ['userId', 'provider', 'details', 'default', 'actions']
    : ['provider', 'details', 'default', 'actions'];
  readonly historyColumns = ['travelId', 'provider', 'amount', 'status', 'date'];

  readonly paymentMethods = signal<PaymentMethod[]>([]);
  readonly history = signal<Payment[]>([]);
  readonly loading = signal(true);
  userIdFilter: number | null = null;

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.paymentService.findAll(this.isAdmin ? (this.userIdFilter ?? undefined) : undefined).subscribe({
      next: (methods) => {
        this.paymentMethods.set(methods);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load payment methods.', 'Dismiss', { duration: 4000 });
      },
    });
    // Everyone can see their own payment history; admins skip it (they
    // review platform-wide finances on the dashboard instead).
    if (!this.isAdmin) {
      this.paymentService.history().subscribe({
        next: (payments) => this.history.set(payments),
        error: () => this.history.set([]),
      });
    }
  }

  details(method: PaymentMethod): string {
    if (method.provider === 'STRIPE') {
      return `${method.brand ?? 'card'} •••• ${method.last4} (exp ${method.expiryMonth}/${method.expiryYear})`;
    }
    return method.payerEmail ?? 'PayPal account';
  }

  openCreateDialog(): void {
    const ref = this.dialog.open(PaymentFormDialogComponent, { width: '480px' });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('Payment method added.', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  confirmDelete(method: PaymentMethod): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete payment method',
        message: `Delete this ${method.provider} payment method? This cannot be undone.`,
      },
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.paymentService.delete(method.id).subscribe({
        next: () => {
          this.snackBar.open('Payment method deleted.', 'Dismiss', { duration: 3000 });
          this.reload();
        },
        error: () => this.snackBar.open('Failed to delete payment method.', 'Dismiss', { duration: 4000 }),
      });
    });
  }
}
