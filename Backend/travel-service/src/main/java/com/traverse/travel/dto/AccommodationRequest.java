package com.traverse.travel.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AccommodationRequest(
        @NotBlank String name,
        @NotBlank String type,
        String address,
        LocalDate checkIn,
        LocalDate checkOut
) {
}
