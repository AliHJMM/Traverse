package com.traverse.travel.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record TravelResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        long durationDays,
        List<DestinationResponse> destinations,
        List<ActivityResponse> activities,
        List<AccommodationResponse> accommodations,
        List<TransportationResponse> transportations,
        Instant createdAt
) {
    public static long computeDurationDays(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
