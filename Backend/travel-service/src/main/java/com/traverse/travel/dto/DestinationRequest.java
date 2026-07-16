package com.traverse.travel.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record DestinationRequest(
        @NotBlank String city,
        @NotBlank String country,
        LocalDate arrivalDate,
        LocalDate departureDate
) {
}
