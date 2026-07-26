package com.traverse.travel.dto;

import java.math.BigDecimal;

/** Ranked-travel row used in the admin overview (top travels). */
public record TravelStatSummary(
        Long travelId,
        String title,
        Long managerId,
        long subscribers,
        BigDecimal income,
        double averageRating
) {
}
