import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';

import { ManagerDetailComponent } from './manager-detail.component';

describe('ManagerDetailComponent', () => {
  let fixture: ComponentFixture<ManagerDetailComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManagerDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '10' } } },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ManagerDetailComponent);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('loads the manager stats from the public snapshot endpoint', () => {
    httpMock.expectOne('/api/travels/stats/manager/10').flush({
      managerId: 10,
      tripsCount: 3,
      activeTravelersCount: 12,
      totalIncome: 1500,
      averageRating: 4.2,
      feedbackCount: 5,
      reportCount: 1,
    });
    expect(fixture.componentInstance.stats()?.tripsCount).toBe(3);
    expect(fixture.componentInstance.loading()).toBeFalse();
  });

  it('flags not-found on error', () => {
    httpMock
      .expectOne('/api/travels/stats/manager/10')
      .flush('nope', { status: 404, statusText: 'Not Found' });
    expect(fixture.componentInstance.notFound()).toBeTrue();
  });
});
