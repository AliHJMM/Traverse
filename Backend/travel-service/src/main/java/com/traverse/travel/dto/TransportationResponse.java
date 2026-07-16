package com.traverse.travel.dto;

import java.time.LocalDateTime;

public record TransportationResponse(Long id, String type, String provider, String fromLocation, String toLocation,
                                      LocalDateTime departureTime, LocalDateTime arrivalTime) {
}
