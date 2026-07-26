import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FeedbackDialogComponent } from './feedback-dialog.component';

describe('FeedbackDialogComponent', () => {
  let component: FeedbackDialogComponent;
  let fixture: ComponentFixture<FeedbackDialogComponent>;
  let httpMock: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FeedbackDialogComponent>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [FeedbackDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { travelId: 1, travelTitle: 'Trip' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FeedbackDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('submits feedback and closes with the result', () => {
    component.form.setValue({ rating: 4, comment: 'Great' });
    component.submit();

    const req = httpMock.expectOne('/api/travels/1/feedback');
    expect(req.request.body).toEqual({ rating: 4, comment: 'Great' });
    req.flush({ id: 7, travelId: 1, travelerId: 2, rating: 4, comment: 'Great', createdAt: '' });

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('sends a null comment when left blank', () => {
    component.form.setValue({ rating: 5, comment: '' });
    component.submit();

    const req = httpMock.expectOne('/api/travels/1/feedback');
    expect(req.request.body).toEqual({ rating: 5, comment: null });
    req.flush({ id: 8, travelId: 1, travelerId: 2, rating: 5, comment: null, createdAt: '' });
  });

  it('surfaces an error and does not close', () => {
    component.form.setValue({ rating: 3, comment: 'x' });
    component.submit();
    httpMock
      .expectOne('/api/travels/1/feedback')
      .flush({ error: 'nope' }, { status: 409, statusText: 'Conflict' });

    expect(component.errorMessage()).toBe('nope');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
