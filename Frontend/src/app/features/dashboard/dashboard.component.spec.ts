import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthService } from '../../core/auth/auth.service';
import { CurrentUser } from '../../core/models/current-user.model';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let httpMock: HttpTestingController;

  function setup(user: CurrentUser | null): ComponentFixture<DashboardComponent> {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser: user } },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => httpMock.verify());

  it('loads traveler stats for a traveler', () => {
    const fixture = setup({ id: 2, email: 't@b.c', role: 'TRAVELER' });
    httpMock.expectOne('/api/payments').flush([
      { id: 1, userId: 2, provider: 'STRIPE', brand: 'visa', last4: '4242', expiryMonth: 1, expiryYear: 2030, payerEmail: null, isDefault: true, createdAt: '' },
    ]);
    httpMock.expectOne('/api/travels/stats/traveler/me').flush({
      travelerId: 2,
      activeTrips: 3,
      cancellations: 1,
      feedbackGiven: 2,
      reportsFiled: 0,
    });
    expect(fixture.componentInstance.travelerStats()?.activeTrips).toBe(3);
    expect(fixture.componentInstance.preferredMethod()?.last4).toBe('4242');
    expect(fixture.componentInstance.loading()).toBeFalse();
  });

  it('loads manager stats for a manager', () => {
    const fixture = setup({ id: 10, email: 'm@b.c', role: 'TRAVEL_MANAGER' });
    httpMock.expectOne('/api/travels/stats/manager/me').flush({
      managerId: 10,
      tripsCount: 4,
      activeTravelersCount: 12,
      totalIncome: 5000,
      averageRating: 4.5,
      feedbackCount: 8,
      reportCount: 0,
    });
    expect(fixture.componentInstance.managerStats()?.tripsCount).toBe(4);
  });

  it('loads the admin overview for an admin', () => {
    const fixture = setup({ id: 1, email: 'a@b.c', role: 'ADMIN' });
    httpMock.expectOne('/api/travels/stats/admin/overview').flush({
      totalManagers: 2,
      totalTravels: 5,
      totalActiveSubscriptions: 9,
      totalIncome: 12000,
      openReports: 1,
      topManagers: [],
      topTravels: [],
      monthlyIncome: [{ month: '2026-07', income: 500 }],
    });
    expect(fixture.componentInstance.adminOverview()?.totalTravels).toBe(5);
    expect(fixture.componentInstance.maxMonthlyIncome()).toBe(500);
  });

  it('stops loading gracefully on error', () => {
    const fixture = setup({ id: 2, email: 't@b.c', role: 'TRAVELER' });
    httpMock.expectOne('/api/payments').flush([]);
    httpMock
      .expectOne('/api/travels/stats/traveler/me')
      .flush('err', { status: 500, statusText: 'Server Error' });
    expect(fixture.componentInstance.loading()).toBeFalse();
  });
});
