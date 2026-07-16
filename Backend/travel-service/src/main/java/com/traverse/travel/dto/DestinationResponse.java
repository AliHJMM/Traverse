package com.traverse.travel.dto;

import java.time.LocalDate;

public record DestinationResponse(Long id, String city, String country, LocalDate arrivalDate, LocalDate departureDate) {
}
