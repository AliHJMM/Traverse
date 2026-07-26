export type PaymentProvider = 'STRIPE' | 'PAYPAL';
export type PaymentStatus = 'SUCCEEDED' | 'FAILED';

export interface PaymentMethod {
  id: number;
  userId: number;
  provider: PaymentProvider;
  brand: string | null;
  last4: string | null;
  expiryMonth: number | null;
  expiryYear: number | null;
  payerEmail: string | null;
  isDefault: boolean;
  createdAt: string;
}

/** The owning user is derived from the auth cookie server-side, never sent. */
export interface CreatePaymentMethodRequest {
  provider: PaymentProvider;
  token: string;
  setDefault: boolean;
}

export interface ChargeRequest {
  travelId: number;
  paymentMethodId: number;
  amount: number;
}

export interface Payment {
  id: number;
  userId: number;
  travelId: number;
  paymentMethodId: number | null;
  provider: PaymentProvider;
  amount: number;
  currency: string;
  status: PaymentStatus;
  externalChargeId: string | null;
  createdAt: string;
}
