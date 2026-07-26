package com.traverse.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ChargeRequest(
        @NotNull Long travelId,
        @NotNull Long paymentMethodId,
        @NotNull @Positive BigDecimal amount
) {
}
