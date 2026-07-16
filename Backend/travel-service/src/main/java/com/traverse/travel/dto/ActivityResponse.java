package com.traverse.travel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ActivityResponse(Long id, String name, String description, String destinationCity, LocalDate date, BigDecimal cost) {
}
