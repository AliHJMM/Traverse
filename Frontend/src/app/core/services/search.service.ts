import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AutocompleteResponse, TravelSearchResult } from '../models/search.model';

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/search';

  search(query: string): Observable<TravelSearchResult[]> {
    return this.http.get<TravelSearchResult[]>(`${this.baseUrl}/travels`, {
      params: new HttpParams().set('q', query),
    });
  }

  autocomplete(query: string): Observable<AutocompleteResponse> {
    return this.http.get<AutocompleteResponse>(`${this.baseUrl}/autocomplete`, {
      params: new HttpParams().set('q', query),
    });
  }
}
