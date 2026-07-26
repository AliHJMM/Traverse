package com.traverse.payment.dto;

import com.traverse.payment.entity.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The owning user is taken from the authenticated principal, never the
 * request body, so a caller can only ever save a payment method for
 * themselves.
 */
public record CreatePaymentMethodRequest(
        @NotNull PaymentProvider provider,
        @NotBlank String token,
        boolean setDefault
) {
}
