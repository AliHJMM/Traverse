import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AdminOverview, ManagerStats, TravelerStats } from '../models/stats.model';

@Injectable({
  providedIn: 'root',
})
export class StatsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/travels/stats';

  adminOverview(): Observable<AdminOverview> {
    return this.http.get<AdminOverview>(`${this.baseUrl}/admin/overview`);
  }

  myManagerStats(): Observable<ManagerStats> {
    return this.http.get<ManagerStats>(`${this.baseUrl}/manager/me`);
  }

  managerStats(managerId: number): Observable<ManagerStats> {
    return this.http.get<ManagerStats>(`${this.baseUrl}/manager/${managerId}`);
  }

  myTravelerStats(): Observable<TravelerStats> {
    return this.http.get<TravelerStats>(`${this.baseUrl}/traveler/me`);
  }
}
