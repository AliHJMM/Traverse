package com.traverse.search.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload travel-service pushes to keep the Elasticsearch index in sync with
 * its Postgres source of truth (on travel create/update, and on reindex).
 */
public record TravelIndexRequest(
        @NotNull Long id,
        String title,
        List<String> destinationCities,
        List<String> destinationCountries,
        List<String> activities,
        List<String> accommodations,
        List<String> transportationTypes,
        LocalDate startDate,
        LocalDate endDate,
        Integer durationDays,
        Long managerId
) {
}
