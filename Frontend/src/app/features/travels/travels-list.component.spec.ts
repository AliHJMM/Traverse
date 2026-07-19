import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { Travel } from '../../core/models/travel.model';
import { TravelsListComponent } from './travels-list.component';

describe('TravelsListComponent', () => {
  let component: TravelsListComponent;
  let fixture: ComponentFixture<TravelsListComponent>;
  let httpMock: HttpTestingController;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const travel: Travel = {
    id: 1,
    title: 'Europe Trip',
    startDate: '2026-08-01',
    endDate: '2026-08-10',
    durationDays: 10,
    destinations: [{ city: 'Paris', country: 'France', arrivalDate: null, departureDate: null }],
    activities: [],
    accommodations: [],
    transportations: [],
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [TravelsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TravelsListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpMock.expectOne('/api/travels').flush([travel]);
  });

  afterEach(() => httpMock.verify());

  it('loads travels on init', () => {
    expect(component.travels()).toEqual([travel]);
  });

  it('summarizes destinations as an arrow-separated route', () => {
    expect(component.destinationSummary(travel)).toBe('Paris');
  });

  it('reloads the list after a successful create dialog', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(travel) } as ReturnType<MatDialog['open']>);

    component.openCreateDialog();
    httpMock.expectOne('/api/travels').flush([travel, { ...travel, id: 2 }]);

    expect(component.travels().length).toBe(2);
  });

  it('deletes a travel after confirmation', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as ReturnType<MatDialog['open']>);

    component.confirmDelete(travel);
    httpMock.expectOne({ method: 'DELETE', url: '/api/travels/1' }).flush(null);
    httpMock.expectOne('/api/travels').flush([]);

    expect(component.travels()).toEqual([]);
  });

  it('does not delete when the confirmation is cancelled', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as ReturnType<MatDialog['open']>);

    component.confirmDelete(travel);
    httpMock.expectNone({ method: 'DELETE', url: '/api/travels/1' });
  });
});
