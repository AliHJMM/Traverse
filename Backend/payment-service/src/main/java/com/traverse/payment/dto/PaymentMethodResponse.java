package com.traverse.payment.dto;

import com.traverse.payment.entity.PaymentProvider;

import java.time.Instant;

public record PaymentMethodResponse(
        Long id,
        Long userId,
        PaymentProvider provider,
        String brand,
        String last4,
        Integer expiryMonth,
        Integer expiryYear,
        String payerEmail,
        boolean isDefault,
        Instant createdAt
) {
}
