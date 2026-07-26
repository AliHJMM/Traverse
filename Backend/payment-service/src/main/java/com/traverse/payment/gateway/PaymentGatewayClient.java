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

    AttachedPaymentMethod attach(Long userId, String token);

    void detach(String externalId);

    /**
     * Charge a previously-saved (tokenized) payment method off-session and
     * return the provider's charge/reference id.
     *
     * @param amountMinor amount in the currency's minor unit (e.g. cents)
     * @throws PaymentGatewayException if the provider declines or errors
     */
    String charge(Long userId, String externalPaymentMethodId, long amountMinor, String currency);
}
