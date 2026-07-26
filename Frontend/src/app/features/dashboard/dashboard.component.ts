import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/models/current-user.model';
import { PaymentMethod } from '../../core/models/payment.model';
import { AdminOverview, ManagerStats, TravelerStats } from '../../core/models/stats.model';
import { PaymentService } from '../../core/services/payment.service';
import { StatsService } from '../../core/services/stats.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DecimalPipe, MatProgressSpinnerModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);
  private readonly statsService = inject(StatsService);
  private readonly paymentService = inject(PaymentService);

  readonly role = signal<Role | null>(this.authService.currentUser?.role ?? null);
  readonly loading = signal(true);

  readonly travelerStats = signal<TravelerStats | null>(null);
  readonly managerStats = signal<ManagerStats | null>(null);
  readonly adminOverview = signal<AdminOverview | null>(null);
  readonly preferredMethod = signal<PaymentMethod | null>(null);

  constructor() {
    this.load();
  }

  private load(): void {
    const role = this.role();
    if (role === 'TRAVELER') {
      // Surface the traveler's preferred (default) payment method alongside
      // their stats; best-effort, never blocks the dashboard.
      this.paymentService.findAll().subscribe({
        next: (methods) => this.preferredMethod.set(methods.find((m) => m.isDefault) ?? methods[0] ?? null),
        error: () => this.preferredMethod.set(null),
      });
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

  /** Largest month income, used to scale the monthly-income bars (min 1). */
  maxMonthlyIncome(): number {
    const months = this.adminOverview()?.monthlyIncome ?? [];
    return Math.max(1, ...months.map((m) => m.income));
  }

  preferredLabel(): string {
    const m = this.preferredMethod();
    if (!m) {
      return 'None';
    }
    return m.provider === 'STRIPE' ? `${m.brand ?? 'Card'} ••${m.last4}` : 'PayPal';
  }
}
