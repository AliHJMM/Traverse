package com.traverse.travel.dto;

import com.traverse.travel.entity.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionResponse(
        Long id,
        Long travelId,
        Long travelerId,
        SubscriptionStatus status,
        Instant createdAt
) {
}
