import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../core/auth/auth.service';
import { Report } from '../../core/models/report.model';
import { ReportService } from '../../core/services/report.service';
import { ReportFormDialogComponent } from './report-form-dialog.component';

@Component({
  selector: 'app-reports',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './reports.component.html',
})
export class ReportsComponent {
  private readonly reportService = inject(ReportService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly isAdmin = this.authService.currentUser?.role === 'ADMIN';
  readonly loading = signal(true);
  readonly reports = signal<Report[]>([]);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    // Admins moderate every report; everyone else sees only what they filed.
    const source$ = this.isAdmin ? this.reportService.all() : this.reportService.mine();
    source$.subscribe({
      next: (reports) => {
        this.reports.set(reports);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load reports.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  openFileDialog(): void {
    const ref = this.dialog.open(ReportFormDialogComponent, { width: '520px', maxWidth: '95vw' });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('Report filed.', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  markReviewed(report: Report): void {
    this.reportService.review(report.id).subscribe({
      next: (updated) => {
        this.reports.update((list) => list.map((r) => (r.id === updated.id ? updated : r)));
        this.snackBar.open('Report marked reviewed.', 'Dismiss', { duration: 3000 });
      },
      error: () => this.snackBar.open('Failed to update report.', 'Dismiss', { duration: 4000 }),
    });
  }
}
