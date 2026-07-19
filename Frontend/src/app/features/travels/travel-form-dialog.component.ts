import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';

import { Travel } from '../../core/models/travel.model';
import { TravelService } from '../../core/services/travel.service';

export interface TravelFormDialogData {
  mode: 'create' | 'edit';
  travel?: Travel;
}

@Component({
  selector: 'app-travel-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
  ],
  templateUrl: './travel-form-dialog.component.html',
})
export class TravelFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly travelService = inject(TravelService);
  private readonly dialogRef = inject(MatDialogRef<TravelFormDialogComponent>);
  readonly data = inject<TravelFormDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isEdit = this.data.mode === 'edit';

  readonly form = this.fb.nonNullable.group({
    title: [this.data.travel?.title ?? '', [Validators.required]],
    startDate: [this.data.travel ? new Date(this.data.travel.startDate) : null, [Validators.required]],
    endDate: [this.data.travel ? new Date(this.data.travel.endDate) : null, [Validators.required]],
    destinations: this.fb.array(
      (this.data.travel?.destinations ?? []).map((d) => this.destinationGroup(d)),
      [Validators.required, Validators.minLength(1)],
    ),
    activities: this.fb.array((this.data.travel?.activities ?? []).map((a) => this.activityGroup(a))),
    accommodations: this.fb.array(
      (this.data.travel?.accommodations ?? []).map((a) => this.accommodationGroup(a)),
    ),
    transportations: this.fb.array(
      (this.data.travel?.transportations ?? []).map((t) => this.transportationGroup(t)),
    ),
  });

  get destinations(): FormArray<FormGroup> {
    return this.form.controls.destinations;
  }

  get activities(): FormArray<FormGroup> {
    return this.form.controls.activities;
  }

  get accommodations(): FormArray<FormGroup> {
    return this.form.controls.accommodations;
  }

  get transportations(): FormArray<FormGroup> {
    return this.form.controls.transportations;
  }

  private destinationGroup(value?: Travel['destinations'][number]) {
    return this.fb.nonNullable.group({
      city: [value?.city ?? '', [Validators.required]],
      country: [value?.country ?? '', [Validators.required]],
      arrivalDate: [value?.arrivalDate ? new Date(value.arrivalDate) : null],
      departureDate: [value?.departureDate ? new Date(value.departureDate) : null],
    });
  }

  private activityGroup(value?: Travel['activities'][number]) {
    return this.fb.nonNullable.group({
      name: [value?.name ?? '', [Validators.required]],
      description: [value?.description ?? ''],
      destinationCity: [value?.destinationCity ?? ''],
      date: [value?.date ? new Date(value.date) : null],
      cost: [value?.cost ?? null],
    });
  }

  private accommodationGroup(value?: Travel['accommodations'][number]) {
    return this.fb.nonNullable.group({
      name: [value?.name ?? '', [Validators.required]],
      type: [value?.type ?? '', [Validators.required]],
      address: [value?.address ?? ''],
      checkIn: [value?.checkIn ? new Date(value.checkIn) : null],
      checkOut: [value?.checkOut ? new Date(value.checkOut) : null],
    });
  }

  private transportationGroup(value?: Travel['transportations'][number]) {
    return this.fb.nonNullable.group({
      type: [value?.type ?? '', [Validators.required]],
      provider: [value?.provider ?? ''],
      fromLocation: [value?.fromLocation ?? '', [Validators.required]],
      toLocation: [value?.toLocation ?? '', [Validators.required]],
      departureTime: [value?.departureTime ? new Date(value.departureTime) : null],
      arrivalTime: [value?.arrivalTime ? new Date(value.arrivalTime) : null],
    });
  }

  addDestination(): void {
    this.destinations.push(this.destinationGroup());
  }

  removeDestination(index: number): void {
    this.destinations.removeAt(index);
  }

  addActivity(): void {
    this.activities.push(this.activityGroup());
  }

  removeActivity(index: number): void {
    this.activities.removeAt(index);
  }

  addAccommodation(): void {
    this.accommodations.push(this.accommodationGroup());
  }

  removeAccommodation(index: number): void {
    this.accommodations.removeAt(index);
  }

  addTransportation(): void {
    this.transportations.push(this.transportationGroup());
  }

  removeTransportation(index: number): void {
    this.transportations.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();

    const request = {
      title: value.title,
      startDate: toDateOnly(value.startDate)!,
      endDate: toDateOnly(value.endDate)!,
      destinations: value.destinations.map((d) => ({
        city: d.city,
        country: d.country,
        arrivalDate: toDateOnly(d.arrivalDate),
        departureDate: toDateOnly(d.departureDate),
      })),
      activities: value.activities.map((a) => ({
        name: a.name,
        description: a.description || null,
        destinationCity: a.destinationCity || null,
        date: toDateOnly(a.date),
        cost: a.cost,
      })),
      accommodations: value.accommodations.map((a) => ({
        name: a.name,
        type: a.type,
        address: a.address || null,
        checkIn: toDateOnly(a.checkIn),
        checkOut: toDateOnly(a.checkOut),
      })),
      transportations: value.transportations.map((t) => ({
        type: t.type,
        provider: t.provider || null,
        fromLocation: t.fromLocation,
        toLocation: t.toLocation,
        departureTime: toDateTime(t.departureTime),
        arrivalTime: toDateTime(t.arrivalTime),
      })),
    };

    const request$ = this.isEdit
      ? this.travelService.update(this.data.travel!.id, request)
      : this.travelService.create(request);

    request$.subscribe({
      next: (travel) => {
        this.saving.set(false);
        this.dialogRef.close(travel);
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

function toDateOnly(value: Date | null): string | null {
  if (!value) {
    return null;
  }
  return value.toISOString().slice(0, 10);
}

function toDateTime(value: Date | null): string | null {
  if (!value) {
    return null;
  }
  return value.toISOString().slice(0, 19);
}
