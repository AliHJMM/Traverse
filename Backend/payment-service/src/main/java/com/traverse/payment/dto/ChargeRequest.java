package com.traverse.payment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * The amount is intentionally NOT part of the request -- it's resolved
 * server-side from the travel's authoritative price so a client can't pay an
 * arbitrary (e.g. under-)amount for a booking.
 */
public record ChargeRequest(
        @NotNull Long travelId,
        @NotNull Long paymentMethodId
) {
}
