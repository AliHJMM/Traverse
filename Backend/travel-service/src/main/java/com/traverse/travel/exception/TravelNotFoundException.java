package com.traverse.travel.exception;

public class TravelNotFoundException extends RuntimeException {

    public TravelNotFoundException(Long id) {
        super("No travel found with id " + id);
    }
}
