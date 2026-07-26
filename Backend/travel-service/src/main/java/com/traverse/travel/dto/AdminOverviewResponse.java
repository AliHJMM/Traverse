package com.traverse.travel.dto;

import java.math.BigDecimal;
import java.util.List;

/** Platform-wide KPIs plus leaderboards for the Admin dashboard. */
public record AdminOverviewResponse(
        long totalManagers,
        long totalTravels,
        long totalActiveSubscriptions,
        BigDecimal totalIncome,
        long openReports,
        List<ManagerStatsResponse> topManagers,
        List<TravelStatSummary> topTravels
) {
}
