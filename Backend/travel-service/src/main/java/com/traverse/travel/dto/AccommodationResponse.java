package com.traverse.travel.dto;

import java.time.LocalDate;

public record AccommodationResponse(Long id, String name, String type, String address, LocalDate checkIn, LocalDate checkOut) {
}
