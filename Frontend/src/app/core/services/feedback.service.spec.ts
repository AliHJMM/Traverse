import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FeedbackService } from './feedback.service';

describe('FeedbackService', () => {
  let service: FeedbackService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FeedbackService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('submit POSTs feedback for a travel', () => {
    service.submit(1, { rating: 5, comment: 'Great' }).subscribe();
    const req = httpMock.expectOne({ method: 'POST', url: '/api/travels/1/feedback' });
    expect(req.request.body).toEqual({ rating: 5, comment: 'Great' });
    req.flush({});
  });

  it('forTravel GETs feedback for a travel', () => {
    service.forTravel(1).subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/1/feedback' }).flush([]);
  });

  it('mine GETs the current traveler feedback', () => {
    service.mine().subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/feedback/mine' }).flush([]);
  });
});
