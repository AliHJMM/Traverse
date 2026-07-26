import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Feedback, FeedbackRequest } from '../models/feedback.model';

@Injectable({
  providedIn: 'root',
})
export class FeedbackService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/travels';

  submit(travelId: number, request: FeedbackRequest): Observable<Feedback> {
    return this.http.post<Feedback>(`${this.baseUrl}/${travelId}/feedback`, request);
  }

  forTravel(travelId: number): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.baseUrl}/${travelId}/feedback`);
  }

  mine(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.baseUrl}/feedback/mine`);
  }
}
