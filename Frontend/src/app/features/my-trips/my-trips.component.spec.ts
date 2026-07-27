import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Travel } from '../../core/models/travel.model';
import { MyTripsComponent } from './my-trips.component';

describe('MyTripsComponent', () => {
  let component: MyTripsComponent;
  let fixture: ComponentFixture<MyTripsComponent>;
  let httpMock: HttpTestingController;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const travel: Travel = {
    id: 1,
    title: 'Europe Trip',
    startDate: '2026-08-01',
    endDate: '2026-08-10',
    durationDays: 10,
    price: 500,
    managerId: 10,
    destinations: [{ city: 'Paris', country: 'France', arrivalDate: null, departureDate: null }],
    activities: [],
    accommodations: [],
    transportations: [],
    createdAt: '2026-01-01T00:00:00Z',
  };

  function flushInit(reviewed = false, paid = false): void {
    httpMock.expectOne('/api/travels/subscriptions/mine').flush([
      { id: 9, travelId: 1, travelerId: 2, status: 'SUBSCRIBED', createdAt: '2026-01-01T00:00:00Z' },
    ]);
    httpMock.expectOne('/api/travels/subscriptions/history').flush([
      { id: 9, travelId: 1, travelerId: 2, status: 'SUBSCRIBED', createdAt: '2026-01-01T00:00:00Z' },
    ]);
    httpMock.expectOne('/api/travels').flush([travel]);
    httpMock
      .expectOne('/api/travels/feedback/mine')
      .flush(reviewed ? [{ id: 3, travelId: 1, travelerId: 2, rating: 5, comment: 'x', createdAt: '' }] : []);
    httpMock
      .expectOne('/api/payments/charges/mine')
      .flush(paid ? [{ id: 5, travelId: 1, status: 'SUCCEEDED', amount: 500 }] : []);
  }

  beforeEach(async () => {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [MyTripsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyTripsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('joins subscriptions with travel details', () => {
    flushInit();
    expect(component.trips().length).toBe(1);
    expect(component.trips()[0].travel.title).toBe('Europe Trip');
    expect(component.trips()[0].reviewed).toBeFalse();
  });

  it('marks a trip reviewed when feedback exists', () => {
    flushInit(true);
    expect(component.trips()[0].reviewed).toBeTrue();
  });

  it('reloads after submitting feedback', () => {
    flushInit();
    dialogSpy.open.and.returnValue({ afterClosed: () => of({ id: 3 }) } as ReturnType<MatDialog['open']>);

    component.leaveFeedback(travel);
    flushInit(true);

    expect(component.trips()[0].reviewed).toBeTrue();
  });

  it('unsubscribes after confirmation', () => {
    flushInit();
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as ReturnType<MatDialog['open']>);

    component.unsubscribe(travel);
    httpMock.expectOne({ method: 'DELETE', url: '/api/travels/1/subscribe' }).flush(null);
    httpMock.expectOne('/api/travels/subscriptions/mine').flush([]);
    httpMock.expectOne('/api/travels/subscriptions/history').flush([
      { id: 9, travelId: 1, travelerId: 2, status: 'CANCELLED', createdAt: '2026-01-01T00:00:00Z' },
    ]);
    httpMock.expectOne('/api/travels').flush([travel]);
    httpMock.expectOne('/api/travels/feedback/mine').flush([]);
    httpMock.expectOne('/api/payments/charges/mine').flush([]);

    expect(component.trips().length).toBe(0);
    expect(component.history()[0].status).toBe('CANCELLED');
  });

  it('marks a trip paid when a succeeded payment exists', () => {
    flushInit(false, true);
    expect(component.trips()[0].paid).toBeTrue();
  });

  it('reloads after a successful payment', () => {
    flushInit();
    expect(component.trips()[0].paid).toBeFalse();
    dialogSpy.open.and.returnValue({ afterClosed: () => of({ id: 5 }) } as ReturnType<MatDialog['open']>);

    component.pay(travel);
    flushInit(false, true);

    expect(component.trips()[0].paid).toBeTrue();
  });
});
