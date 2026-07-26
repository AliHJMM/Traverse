import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { ReportFormDialogComponent } from './report-form-dialog.component';

describe('ReportFormDialogComponent', () => {
  let component: ReportFormDialogComponent;
  let fixture: ComponentFixture<ReportFormDialogComponent>;
  let httpMock: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ReportFormDialogComponent>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      imports: [ReportFormDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportFormDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('does not submit when required fields are missing', () => {
    component.submit();
    expect(component.form.invalid).toBeTrue();
    httpMock.expectNone('/api/travels/reports');
  });

  it('files a report and closes with the result', () => {
    component.form.setValue({ subjectType: 'MANAGER', subjectId: 10, travelId: null, reason: 'Rude' });
    component.submit();

    const req = httpMock.expectOne('/api/travels/reports');
    expect(req.request.body).toEqual({
      subjectType: 'MANAGER',
      subjectId: 10,
      travelId: null,
      reason: 'Rude',
    });
    req.flush({ id: 1, status: 'OPEN' });

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('shows an error and stays open on failure', () => {
    component.form.setValue({ subjectType: 'TRAVELER', subjectId: 3, travelId: 7, reason: 'x' });
    component.submit();
    httpMock
      .expectOne('/api/travels/reports')
      .flush({ error: 'bad' }, { status: 400, statusText: 'Bad Request' });

    expect(component.errorMessage()).toBe('bad');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
