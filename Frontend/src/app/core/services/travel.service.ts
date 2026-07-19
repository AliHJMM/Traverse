import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { NearbyDestination, Travel, TravelRequest } from '../models/travel.model';

@Injectable({
  providedIn: 'root',
})
export class TravelService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/travels';

  findAll(): Observable<Travel[]> {
    return this.http.get<Travel[]>(this.baseUrl);
  }

  findById(id: number): Observable<Travel> {
    return this.http.get<Travel>(`${this.baseUrl}/${id}`);
  }

  create(request: TravelRequest): Observable<Travel> {
    return this.http.post<Travel>(this.baseUrl, request);
  }

  update(id: number, request: TravelRequest): Observable<Travel> {
    return this.http.put<Travel>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  nearbyDestinations(city: string): Observable<NearbyDestination[]> {
    return this.http.get<NearbyDestination[]>(`${this.baseUrl}/destinations/${encodeURIComponent(city)}/nearby`);
  }
}
