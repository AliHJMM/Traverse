import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { BrowseComponent } from './browse.component';

describe('BrowseComponent', () => {
  let fixture: ComponentFixture<BrowseComponent>;
  let component: BrowseComponent;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BrowseComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(BrowseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // Initial browse data
    httpMock.expectOne('/api/travels/recommendations').flush([]);
    httpMock.expectOne('/api/travels').flush([
      {
        id: 1,
        title: 'Trip',
        startDate: '2026-09-01',
        endDate: '2026-09-05',
        durationDays: 5,
        price: 500,
        managerId: 10,
        destinations: [{ city: 'Paris', country: 'France', arrivalDate: null, departureDate: null }],
        activities: [],
        accommodations: [],
        transportations: [],
        createdAt: '2026-07-01T00:00:00Z',
      },
    ]);
    httpMock.expectOne('/api/travels/subscriptions/mine').flush([]);
  });

  afterEach(() => httpMock.verify());

  it('loads browse data on init', () => {
    expect(component.allTravels().length).toBe(1);
    expect(component.loading()).toBeFalse();
  });

  it('runs a search (with autocomplete) after debounce', fakeAsync(() => {
    component.searchControl.setValue('paris');
    tick(300);

    httpMock.expectOne('/api/search/autocomplete?q=paris').flush({ suggestions: ['Paris'] });
    httpMock.expectOne('/api/search/travels?q=paris').flush([
      {
        id: '1',
        title: 'Trip',
        destinationCities: ['Paris'],
        destinationCountries: ['France'],
        activities: ['museum'],
        accommodations: [],
        transportationTypes: [],
        startDate: '2026-09-01',
        endDate: '2026-09-05',
        durationDays: 5,
        managerId: 10,
      },
    ]);

    expect(component.hasQuery()).toBeTrue();
    expect(component.results().length).toBe(1);
    expect(component.suggestions()).toEqual(['Paris']);
  }));

  it('subscribes to a travel and marks it subscribed', () => {
    component.subscribe(1);
    httpMock.expectOne('/api/travels/1/subscribe').flush({
      id: 5,
      travelId: 1,
      travelerId: 2,
      status: 'SUBSCRIBED',
      createdAt: '2026-07-01T00:00:00Z',
    });
    expect(component.isSubscribed(1)).toBeTrue();
  });

  it('clears results when the query is emptied', fakeAsync(() => {
    component.searchControl.setValue('x');
    tick(300);
    httpMock.expectOne('/api/search/autocomplete?q=x').flush({ suggestions: [] });
    httpMock.expectOne('/api/search/travels?q=x').flush([]);

    component.searchControl.setValue('');
    tick(300);
    expect(component.hasQuery()).toBeFalse();
    expect(component.results().length).toBe(0);
  }));
});
