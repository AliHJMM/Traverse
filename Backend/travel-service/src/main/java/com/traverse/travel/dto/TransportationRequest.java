package com.traverse.travel.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TransportationRequest(
        @NotBlank String type,
        String provider,
        @NotBlank String fromLocation,
        @NotBlank String toLocation,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime
) {
}
