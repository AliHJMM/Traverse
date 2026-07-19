import { Injectable } from '@angular/core';
import type { Stripe } from '@stripe/stripe-js';
import { loadStripe } from '@stripe/stripe-js';

import { getStripePublishableKey } from '../config/env';

/**
 * Thin wrapper around @stripe/stripe-js's loadStripe so components can
 * depend on an injectable (and therefore mockable) service instead of
 * calling the imported function directly -- tests would otherwise trigger
 * a real network fetch of Stripe's hosted script.
 */
@Injectable({
  providedIn: 'root',
})
export class StripeLoaderService {
  load(): Promise<Stripe | null> {
    return loadStripe(getStripePublishableKey());
  }
}
