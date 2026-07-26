package com.traverse.travel.exception;

/**
 * Thrown when a Travel Manager tries to modify a travel they don't own.
 * Admins bypass this check; managers may only manage their own listings.
 */
public class TravelAccessDeniedException extends RuntimeException {

    public TravelAccessDeniedException(Long travelId) {
        super("You do not have permission to manage travel " + travelId);
    }
}
