import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { User } from '../../core/models/user.model';
import { UserService } from '../../core/services/user.service';

export interface UserFormDialogData {
  mode: 'create' | 'edit';
  user?: User;
}

@Component({
  selector: 'app-user-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  templateUrl: './user-form-dialog.component.html',
})
export class UserFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly dialogRef = inject(MatDialogRef<UserFormDialogComponent>);
  readonly data = inject<UserFormDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isEdit = this.data.mode === 'edit';

  readonly form = this.fb.nonNullable.group({
    email: [this.data.user?.email ?? '', [Validators.required, Validators.email]],
    password: ['', this.isEdit ? [] : [Validators.required, Validators.minLength(8)]],
    role: [this.data.user?.role ?? 'USER', [Validators.required]],
    fullName: [this.data.user?.fullName ?? '', [Validators.required]],
    phone: [this.data.user?.phone ?? ''],
    address: [this.data.user?.address ?? ''],
    enabled: [this.data.user?.enabled ?? true],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();

    const request$ = this.isEdit
      ? this.userService.update(this.data.user!.id, {
          email: value.email,
          role: value.role,
          fullName: value.fullName,
          phone: value.phone || undefined,
          address: value.address || undefined,
          enabled: value.enabled,
        })
      : this.userService.create({
          email: value.email,
          password: value.password,
          role: value.role,
          fullName: value.fullName,
          phone: value.phone || undefined,
          address: value.address || undefined,
        });

    request$.subscribe({
      next: (user) => {
        this.saving.set(false);
        this.dialogRef.close(user);
      },
      error: (error) => {
        this.saving.set(false);
        this.errorMessage.set(error?.error?.error ?? 'Something went wrong. Please try again.');
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
