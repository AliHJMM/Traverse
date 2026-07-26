import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { Report } from '../../core/models/report.model';
import { ReportsComponent } from './reports.component';

describe('ReportsComponent', () => {
  let component: ReportsComponent;
  let fixture: ComponentFixture<ReportsComponent>;
  let httpMock: HttpTestingController;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const report: Report = {
    id: 1,
    reporterId: 2,
    subjectType: 'MANAGER',
    subjectId: 10,
    travelId: null,
    reason: 'Rude',
    status: 'OPEN',
    createdAt: '2026-01-01T00:00:00Z',
  };

  function configure(role: 'ADMIN' | 'TRAVELER') {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      imports: [ReportsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: AuthService, useValue: { currentUser: { id: 1, email: 'a@b.c', role } } },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReportsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => httpMock.verify());

  it('loads all reports for an admin', () => {
    configure('ADMIN');
    httpMock.expectOne('/api/travels/reports').flush([report]);
    expect(component.reports().length).toBe(1);
  });

  it('loads only own reports for a traveler', () => {
    configure('TRAVELER');
    httpMock.expectOne('/api/travels/reports/mine').flush([report]);
    expect(component.isAdmin).toBeFalse();
  });

  it('marks a report reviewed (admin)', () => {
    configure('ADMIN');
    httpMock.expectOne('/api/travels/reports').flush([report]);

    component.markReviewed(report);
    httpMock
      .expectOne({ method: 'PATCH', url: '/api/travels/reports/1/review' })
      .flush({ ...report, status: 'REVIEWED' });

    expect(component.reports()[0].status).toBe('REVIEWED');
  });

  it('reloads after filing a report', () => {
    configure('TRAVELER');
    httpMock.expectOne('/api/travels/reports/mine').flush([]);
    dialogSpy.open.and.returnValue({ afterClosed: () => of(report) } as ReturnType<MatDialog['open']>);

    component.openFileDialog();
    httpMock.expectOne('/api/travels/reports/mine').flush([report]);

    expect(component.reports().length).toBe(1);
  });
});
