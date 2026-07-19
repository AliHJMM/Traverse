import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { Travel } from '../../core/models/travel.model';
import { TravelService } from '../../core/services/travel.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { TravelFormDialogComponent } from './travel-form-dialog.component';

@Component({
  selector: 'app-travels-list',
  // MatDialogModule intentionally not imported -- see UsersListComponent
  // for why (it would shadow the app-wide MatDialog instance in tests).
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './travels-list.component.html',
})
export class TravelsListComponent {
  private readonly travelService = inject(TravelService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['title', 'dates', 'duration', 'destinations', 'actions'];
  readonly travels = signal<Travel[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.travelService.findAll().subscribe({
      next: (travels) => {
        this.travels.set(travels);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load travels.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  destinationSummary(travel: Travel): string {
    return travel.destinations.map((d) => d.city).join(' -> ');
  }

  openCreateDialog(): void {
    const ref = this.dialog.open(TravelFormDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      data: { mode: 'create' },
    });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('Travel created.', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  openEditDialog(travel: Travel): void {
    const ref = this.dialog.open(TravelFormDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      data: { mode: 'edit', travel },
    });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('Travel updated.', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  confirmDelete(travel: Travel): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: { title: 'Delete travel', message: `Delete "${travel.title}"? This cannot be undone.` },
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.travelService.delete(travel.id).subscribe({
        next: () => {
          this.snackBar.open('Travel deleted.', 'Dismiss', { duration: 3000 });
          this.reload();
        },
        error: () => this.snackBar.open('Failed to delete travel.', 'Dismiss', { duration: 4000 }),
      });
    });
  }
}
