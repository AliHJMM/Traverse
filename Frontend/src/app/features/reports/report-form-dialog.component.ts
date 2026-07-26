import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { ReportSubjectType } from '../../core/models/report.model';
import { ReportService } from '../../core/services/report.service';

@Component({
  selector: 'app-report-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './report-form-dialog.component.html',
})
export class ReportFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly reportService = inject(ReportService);
  private readonly dialogRef = inject(MatDialogRef<ReportFormDialogComponent>);

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly subjectTypes: ReportSubjectType[] = ['MANAGER', 'TRAVELER'];

  readonly form = this.fb.nonNullable.group({
    subjectType: ['MANAGER' as ReportSubjectType, [Validators.required]],
    subjectId: [null as number | null, [Validators.required, Validators.min(1)]],
    travelId: [null as number | null],
    reason: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();

    this.reportService
      .file({
        subjectType: value.subjectType,
        subjectId: value.subjectId!,
        travelId: value.travelId,
        reason: value.reason,
      })
      .subscribe({
        next: (report) => {
          this.saving.set(false);
          this.dialogRef.close(report);
        },
        error: (error) => {
          this.saving.set(false);
          this.errorMessage.set(error?.error?.error ?? 'Failed to file report.');
        },
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
