package com.traverse.payment.dto;

import com.traverse.payment.entity.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentMethodRequest(
        @NotNull Long userId,
        @NotNull PaymentProvider provider,
        @NotBlank String token,
        boolean setDefault
) {
}
