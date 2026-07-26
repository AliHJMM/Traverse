import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { FeedbackService } from '../../core/services/feedback.service';

export interface FeedbackDialogData {
  travelId: number;
  travelTitle: string;
}

@Component({
  selector: 'app-feedback-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './feedback-dialog.component.html',
})
export class FeedbackDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly feedbackService = inject(FeedbackService);
  private readonly dialogRef = inject(MatDialogRef<FeedbackDialogComponent>);
  readonly data = inject<FeedbackDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly ratings = [5, 4, 3, 2, 1];

  readonly form = this.fb.nonNullable.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.maxLength(2000)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();

    this.feedbackService
      .submit(this.data.travelId, { rating: value.rating, comment: value.comment || null })
      .subscribe({
        next: (feedback) => {
          this.saving.set(false);
          this.dialogRef.close(feedback);
        },
        error: (error) => {
          this.saving.set(false);
          this.errorMessage.set(error?.error?.error ?? 'Failed to submit feedback.');
        },
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
