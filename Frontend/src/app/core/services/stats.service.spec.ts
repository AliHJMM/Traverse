import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { StatsService } from './stats.service';

describe('StatsService', () => {
  let service: StatsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StatsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('adminOverview GETs the admin overview', () => {
    service.adminOverview().subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/stats/admin/overview' }).flush({});
  });

  it('myManagerStats GETs the manager self stats', () => {
    service.myManagerStats().subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/stats/manager/me' }).flush({});
  });

  it('managerStats GETs a specific manager snapshot', () => {
    service.managerStats(10).subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/stats/manager/10' }).flush({});
  });

  it('myTravelerStats GETs the traveler self stats', () => {
    service.myTravelerStats().subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/stats/traveler/me' }).flush({});
  });
});
