import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SubscriptionService } from './subscription.service';

describe('SubscriptionService', () => {
  let service: SubscriptionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SubscriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('subscribe POSTs to the subscribe endpoint', () => {
    service.subscribe(1).subscribe();
    httpMock.expectOne({ method: 'POST', url: '/api/travels/1/subscribe' }).flush({});
  });

  it('unsubscribe DELETEs the subscribe endpoint', () => {
    service.unsubscribe(1).subscribe();
    httpMock.expectOne({ method: 'DELETE', url: '/api/travels/1/subscribe' }).flush(null);
  });

  it('subscribers GETs the subscribers of a travel', () => {
    service.subscribers(1).subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/1/subscribers' }).flush([]);
  });

  it('removeSubscriber DELETEs a specific subscriber', () => {
    service.removeSubscriber(1, 2).subscribe();
    httpMock.expectOne({ method: 'DELETE', url: '/api/travels/1/subscribers/2' }).flush(null);
  });

  it('mine GETs the current traveler subscriptions', () => {
    service.mine().subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/subscriptions/mine' }).flush([]);
  });
});
