package com.traverse.travel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateTravelRequest(
        @NotBlank String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty @Valid List<DestinationRequest> destinations,
        @Valid List<ActivityRequest> activities,
        @Valid List<AccommodationRequest> accommodations,
        @Valid List<TransportationRequest> transportations
) {
}
