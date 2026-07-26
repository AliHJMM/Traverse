package com.traverse.travel.exception;

/**
 * Thrown when a traveler tries to subscribe or unsubscribe within the
 * 3-day-before-departure cutoff window.
 */
public class SubscriptionCutoffException extends RuntimeException {

    public SubscriptionCutoffException() {
        super("Subscriptions can no longer be changed within 3 days of departure");
    }
}
