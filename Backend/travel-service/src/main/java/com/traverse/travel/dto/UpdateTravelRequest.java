package com.traverse.travel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * A PUT replaces the travel's full nested state (destinations/activities/
 * accommodations/transportations) rather than patching individual entries --
 * simpler semantics for a nested aggregate than trying to diff child lists.
 */
public record UpdateTravelRequest(
        @NotBlank String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty @Valid List<DestinationRequest> destinations,
        @Valid List<ActivityRequest> activities,
        @Valid List<AccommodationRequest> accommodations,
        @Valid List<TransportationRequest> transportations
) {
    @AssertTrue(message = "endDate must not be before startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
