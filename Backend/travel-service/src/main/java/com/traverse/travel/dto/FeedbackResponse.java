package com.traverse.travel.dto;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        Long travelId,
        Long travelerId,
        int rating,
        String comment,
        Instant createdAt
) {
}
