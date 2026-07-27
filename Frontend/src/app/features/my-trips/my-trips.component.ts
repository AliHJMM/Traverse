import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Travel } from '../../core/models/travel.model';
import { FeedbackService } from '../../core/services/feedback.service';
import { PaymentService } from '../../core/services/payment.service';
import { SubscriptionService } from '../../core/services/subscription.service';
import { TravelService } from '../../core/services/travel.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { PayDialogComponent } from '../payments/pay-dialog.component';
import { FeedbackDialogComponent } from './feedback-dialog.component';

interface TripView {
  travel: Travel;
  reviewed: boolean;
  paid: boolean;
}

interface HistoryEntry {
  travelId: number;
  title: string;
  startDate: string | null;
  endDate: string | null;
  status: string;
}

@Component({
  selector: 'app-my-trips',
  imports: [
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    RouterLink,
  ],
  templateUrl: './my-trips.component.html',
})
export class MyTripsComponent {
  private readonly travelService = inject(TravelService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly paymentService = inject(PaymentService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly trips = signal<TripView[]>([]);
  readonly history = signal<HistoryEntry[]>([]);
  readonly isEmpty = computed(() => !this.loading() && this.trips().length === 0);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    forkJoin({
      subscriptions: this.subscriptionService.mine(),
      history: this.subscriptionService.history(),
      travels: this.travelService.findAll(),
      feedback: this.feedbackService.mine(),
      payments: this.paymentService.history(),
    }).subscribe({
      next: ({ subscriptions, history, travels, feedback, payments }) => {
        const byId = new Map(travels.map((t) => [t.id, t]));
        const reviewedIds = new Set(feedback.map((f) => f.travelId));
        const paidIds = new Set(payments.filter((p) => p.status === 'SUCCEEDED').map((p) => p.travelId));
        this.history.set(
          history.map((s) => {
            const t = byId.get(s.travelId);
            return {
              travelId: s.travelId,
              title: t?.title ?? `Trip #${s.travelId}`,
              startDate: t?.startDate ?? null,
              endDate: t?.endDate ?? null,
              status: s.status,
            };
          }),
        );
        const active = subscriptions
          .filter((s) => s.status === 'SUBSCRIBED')
          .map((s) => byId.get(s.travelId))
          .filter((t): t is Travel => !!t)
          .map((travel) => ({
            travel,
            reviewed: reviewedIds.has(travel.id),
            paid: paidIds.has(travel.id),
          }));
        this.trips.set(active);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load your trips.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  destinationSummary(travel: Travel): string {
    return travel.destinations.map((d) => d.city).join(', ');
  }

  pay(travel: Travel): void {
    const ref = this.dialog.open(PayDialogComponent, {
      width: '440px',
      data: { travelId: travel.id, travelTitle: travel.title, amount: travel.price },
    });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('Payment successful.', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  leaveFeedback(travel: Travel): void {
    const ref = this.dialog.open(FeedbackDialogComponent, {
      width: '480px',
      data: { travelId: travel.id, travelTitle: travel.title },
    });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('Thanks for your feedback!', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  unsubscribe(travel: Travel): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: { title: 'Unsubscribe', message: `Leave "${travel.title}"?` },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.subscriptionService.unsubscribe(travel.id).subscribe({
        next: () => {
          this.snackBar.open('Unsubscribed.', 'Dismiss', { duration: 3000 });
          this.reload();
        },
        error: () => this.snackBar.open('Failed to unsubscribe.', 'Dismiss', { duration: 4000 }),
      });
    });
  }
}
