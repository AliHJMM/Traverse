package com.traverse.payment.dto;

import com.traverse.payment.entity.PaymentProvider;
import com.traverse.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long userId,
        Long travelId,
        Long paymentMethodId,
        PaymentProvider provider,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String externalChargeId,
        Instant createdAt
) {
}
