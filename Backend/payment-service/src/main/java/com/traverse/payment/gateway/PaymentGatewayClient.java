package com.traverse.payment.gateway;

import com.traverse.payment.entity.PaymentProvider;

/**
 * One implementation per provider (Stripe/PayPal). Never handles raw card
 * numbers -- "token" is always an opaque id the provider's own client-side
 * SDK (Stripe.js / PayPal JS SDK) already produced before this backend ever
 * sees it; we just attach/validate it and detach it later.
 */
public interface PaymentGatewayClient {

    PaymentProvider provider();

    AttachedPaymentMethod attach(String token);

    void detach(String externalId);
}
