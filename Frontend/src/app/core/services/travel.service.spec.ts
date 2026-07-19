import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Travel, TravelRequest } from '../models/travel.model';
import { TravelService } from './travel.service';

describe('TravelService', () => {
  let service: TravelService;
  let httpMock: HttpTestingController;

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

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TravelService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('findAll GETs /api/travels', () => {
    service.findAll().subscribe((travels) => expect(travels).toEqual([travel]));
    httpMock.expectOne({ method: 'GET', url: '/api/travels' }).flush([travel]);
  });

  it('findById GETs /api/travels/:id', () => {
    service.findById(1).subscribe((result) => expect(result).toEqual(travel));
    httpMock.expectOne({ method: 'GET', url: '/api/travels/1' }).flush(travel);
  });

  it('create POSTs to /api/travels', () => {
    const request: TravelRequest = {
      title: 'Europe Trip',
      startDate: '2026-08-01',
      endDate: '2026-08-10',
      destinations: travel.destinations,
      activities: [],
      accommodations: [],
      transportations: [],
    };
    service.create(request).subscribe((result) => expect(result).toEqual(travel));

    const req = httpMock.expectOne({ method: 'POST', url: '/api/travels' });
    expect(req.request.body).toEqual(request);
    req.flush(travel);
  });

  it('update PUTs to /api/travels/:id', () => {
    const request: TravelRequest = {
      title: 'Europe Trip Extended',
      startDate: '2026-08-01',
      endDate: '2026-08-12',
      destinations: travel.destinations,
      activities: [],
      accommodations: [],
      transportations: [],
    };
    service.update(1, request).subscribe();
    httpMock.expectOne({ method: 'PUT', url: '/api/travels/1' }).flush({ ...travel, ...request });
  });

  it('delete DELETEs /api/travels/:id', () => {
    service.delete(1).subscribe();
    httpMock.expectOne({ method: 'DELETE', url: '/api/travels/1' }).flush(null);
  });

  it('nearbyDestinations GETs the graph-backed endpoint', () => {
    service.nearbyDestinations('Paris').subscribe((results) => {
      expect(results).toEqual([{ city: 'Rome', country: 'Italy' }]);
    });
    httpMock.expectOne({ method: 'GET', url: '/api/travels/destinations/Paris/nearby' }).flush([
      { city: 'Rome', country: 'Italy' },
    ]);
  });
});
