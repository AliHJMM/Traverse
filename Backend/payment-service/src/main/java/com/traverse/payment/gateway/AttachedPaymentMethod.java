package com.traverse.payment.gateway;

public record AttachedPaymentMethod(
        String externalId,
        String brand,
        String last4,
        Integer expiryMonth,
        Integer expiryYear,
        String payerEmail
) {
}
