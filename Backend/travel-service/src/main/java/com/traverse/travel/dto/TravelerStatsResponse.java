package com.traverse.travel.dto;

/** Personal activity snapshot for a Traveler's dashboard. */
public record TravelerStatsResponse(
        Long travelerId,
        long activeTrips,
        long cancellations,
        long feedbackGiven,
        long reportsFiled
) {
}
