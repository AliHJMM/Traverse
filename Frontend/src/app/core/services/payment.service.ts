import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ChargeRequest,
  CreatePaymentMethodRequest,
  Payment,
  PaymentMethod,
} from '../models/payment.model';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/payments';

  /**
   * Non-admins always receive only their own methods (server-enforced); the
   * userId param is honoured for admins doing oversight.
   */
  findAll(userId?: number): Observable<PaymentMethod[]> {
    const params = userId != null ? new HttpParams().set('userId', userId) : undefined;
    return this.http.get<PaymentMethod[]>(this.baseUrl, { params });
  }

  create(request: CreatePaymentMethodRequest): Observable<PaymentMethod> {
    return this.http.post<PaymentMethod>(this.baseUrl, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Charge one of the caller's own saved methods for a travel booking. */
  charge(request: ChargeRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.baseUrl}/charges`, request);
  }

  /** The caller's own payment history. */
  history(): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.baseUrl}/charges/mine`);
  }
}
