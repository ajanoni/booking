package com.ajanoni.exception;

import static java.lang.String.format;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(String id) {
        super(format("Reserve not found for id %s", id));
    }
}
