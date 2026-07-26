package com.traverse.travel.exception;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(Long id) {
        super("Report " + id + " not found");
    }
}
