import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CreatePaymentMethodRequest, PaymentMethod } from '../models/payment.model';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/payments';

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
}
