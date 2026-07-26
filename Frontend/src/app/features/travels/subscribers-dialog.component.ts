import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Subscriber } from '../../core/models/subscription.model';
import { SubscriptionService } from '../../core/services/subscription.service';

export interface SubscribersDialogData {
  travelId: number;
  travelTitle: string;
}

@Component({
  selector: 'app-subscribers-dialog',
  imports: [DatePipe, MatButtonModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './subscribers-dialog.component.html',
})
export class SubscribersDialogComponent {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);
  readonly data = inject<SubscribersDialogData>(MAT_DIALOG_DATA);

  readonly loading = signal(true);
  readonly subscribers = signal<Subscriber[]>([]);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.subscriptionService.subscribers(this.data.travelId).subscribe({
      next: (subs) => {
        this.subscribers.set(subs);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load subscribers.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  remove(travelerId: number): void {
    this.subscriptionService.removeSubscriber(this.data.travelId, travelerId).subscribe({
      next: () => {
        this.subscribers.update((list) => list.filter((s) => s.travelerId !== travelerId));
        this.snackBar.open('Subscriber removed.', 'Dismiss', { duration: 3000 });
      },
      error: () => this.snackBar.open('Failed to remove subscriber.', 'Dismiss', { duration: 4000 }),
    });
  }
}
