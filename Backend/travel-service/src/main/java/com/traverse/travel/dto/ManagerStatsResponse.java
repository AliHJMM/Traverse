package com.traverse.travel.dto;

import java.math.BigDecimal;

/**
 * Performance snapshot for a Travel Manager. Used both on the manager's own
 * dashboard and (for the public-facing subset) when a traveler views a
 * manager before joining one of their trips.
 */
public record ManagerStatsResponse(
        Long managerId,
        long tripsCount,
        long activeTravelersCount,
        BigDecimal totalIncome,
        double averageRating,
        long feedbackCount,
        long reportCount
) {
}
