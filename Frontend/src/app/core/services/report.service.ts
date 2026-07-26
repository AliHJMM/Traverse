import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Report, ReportRequest } from '../models/report.model';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/travels/reports';

  file(request: ReportRequest): Observable<Report> {
    return this.http.post<Report>(this.baseUrl, request);
  }

  /** All reports (admin only). */
  all(): Observable<Report[]> {
    return this.http.get<Report[]>(this.baseUrl);
  }

  mine(): Observable<Report[]> {
    return this.http.get<Report[]>(`${this.baseUrl}/mine`);
  }

  review(id: number): Observable<Report> {
    return this.http.patch<Report>(`${this.baseUrl}/${id}/review`, {});
  }
}
