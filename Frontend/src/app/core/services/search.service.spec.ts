import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SearchService } from './search.service';

describe('SearchService', () => {
  let service: SearchService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SearchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('search GETs the travels search endpoint with the query', () => {
    service.search('paris').subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/search/travels?q=paris' }).flush([]);
  });

  it('autocomplete GETs the autocomplete endpoint with the query', () => {
    service.autocomplete('par').subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/search/autocomplete?q=par' }).flush({ suggestions: [] });
  });
});
