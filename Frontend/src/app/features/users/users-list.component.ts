import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { User } from '../../core/models/user.model';
import { UserService } from '../../core/services/user.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { UserFormDialogComponent } from './user-form-dialog.component';

@Component({
  selector: 'app-users-list',
  // MatDialogModule is deliberately not imported here: this component only
  // needs the injectable MatDialog service (for that, importing the module
  // isn't required), and the module's own providers would otherwise create
  // a component-local MatDialog instance that shadows the app-wide one --
  // including the one tests override via TestBed.
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './users-list.component.html',
})
export class UsersListComponent {
  private readonly userService = inject(UserService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['fullName', 'email', 'role', 'enabled', 'actions'];
  readonly users = signal<User[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.userService.findAll().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load users.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  initials(fullName: string): string {
    return fullName
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  openCreateDialog(): void {
    const ref = this.dialog.open(UserFormDialogComponent, { width: '480px', data: { mode: 'create' } });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('User created.', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  openEditDialog(user: User): void {
    const ref = this.dialog.open(UserFormDialogComponent, { width: '480px', data: { mode: 'edit', user } });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('User updated.', 'Dismiss', { duration: 3000 });
        this.reload();
      }
    });
  }

  confirmDelete(user: User): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: { title: 'Delete user', message: `Delete ${user.email}? This cannot be undone.` },
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.userService.delete(user.id).subscribe({
        next: () => {
          this.snackBar.open('User deleted.', 'Dismiss', { duration: 3000 });
          this.reload();
        },
        error: () => this.snackBar.open('Failed to delete user.', 'Dismiss', { duration: 4000 }),
      });
    });
  }
}
