package com.traverse.travel.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ActivityRequest(
        @NotBlank String name,
        String description,
        String destinationCity,
        LocalDate date,
        BigDecimal cost
) {
}
