export type PaymentProvider = 'STRIPE' | 'PAYPAL';

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

export interface CreatePaymentMethodRequest {
  userId: number;
  provider: PaymentProvider;
  token: string;
  setDefault: boolean;
}
