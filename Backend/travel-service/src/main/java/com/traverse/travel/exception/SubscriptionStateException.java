package com.traverse.travel.exception;

/**
 * Thrown for invalid subscription state transitions -- e.g. subscribing when
 * already subscribed, or unsubscribing when not subscribed.
 */
public class SubscriptionStateException extends RuntimeException {

    public SubscriptionStateException(String message) {
        super(message);
    }
}
