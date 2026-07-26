import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Subscriber, Subscription } from '../models/subscription.model';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/travels';

  subscribe(travelId: number): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.baseUrl}/${travelId}/subscribe`, {});
  }

  unsubscribe(travelId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${travelId}/subscribe`);
  }

  /** Subscribers of a travel (manager/admin only). */
  subscribers(travelId: number): Observable<Subscriber[]> {
    return this.http.get<Subscriber[]>(`${this.baseUrl}/${travelId}/subscribers`);
  }

  removeSubscriber(travelId: number, travelerId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${travelId}/subscribers/${travelerId}`);
  }

  /** Current traveler's own subscriptions. */
  mine(): Observable<Subscription[]> {
    return this.http.get<Subscription[]>(`${this.baseUrl}/subscriptions/mine`);
  }
}
