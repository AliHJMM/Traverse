import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/models/current-user.model';
import { AdminOverview, ManagerStats, TravelerStats } from '../../core/models/stats.model';
import { StatsService } from '../../core/services/stats.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DecimalPipe, MatProgressSpinnerModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);
  private readonly statsService = inject(StatsService);

  readonly role = signal<Role | null>(this.authService.currentUser?.role ?? null);
  readonly loading = signal(true);

  readonly travelerStats = signal<TravelerStats | null>(null);
  readonly managerStats = signal<ManagerStats | null>(null);
  readonly adminOverview = signal<AdminOverview | null>(null);

  constructor() {
    this.load();
  }

  private load(): void {
    const role = this.role();
    if (role === 'TRAVELER') {
      this.statsService.myTravelerStats().subscribe({
        next: (s) => this.done(() => this.travelerStats.set(s)),
        error: () => this.done(),
      });
    } else if (role === 'TRAVEL_MANAGER') {
      this.statsService.myManagerStats().subscribe({
        next: (s) => this.done(() => this.managerStats.set(s)),
        error: () => this.done(),
      });
    } else if (role === 'ADMIN') {
      this.statsService.adminOverview().subscribe({
        next: (s) => this.done(() => this.adminOverview.set(s)),
        error: () => this.done(),
      });
    } else {
      this.loading.set(false);
    }
  }

  private done(apply?: () => void): void {
    apply?.();
    this.loading.set(false);
  }
}
