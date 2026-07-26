import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ManagerStats } from '../../core/models/stats.model';
import { StatsService } from '../../core/services/stats.service';

/**
 * Public-facing Travel Manager profile: a traveler can review a manager's
 * track record (trips run, active travelers, average rating, and how many
 * reports have been filed against them) before subscribing.
 */
@Component({
  selector: 'app-manager-detail',
  imports: [CurrencyPipe, DecimalPipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './manager-detail.component.html',
})
export class ManagerDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly statsService = inject(StatsService);

  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly stats = signal<ManagerStats | null>(null);
  readonly managerId = Number(this.route.snapshot.paramMap.get('id'));

  constructor() {
    this.statsService.managerStats(this.managerId).subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }
}
