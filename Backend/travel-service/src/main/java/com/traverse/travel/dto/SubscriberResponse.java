package com.traverse.travel.dto;

import java.time.Instant;

/** A subscriber on a manager's travel (traveler id + when they joined). */
public record SubscriberResponse(
        Long travelerId,
        Instant subscribedAt
) {
}
