package com.ajanoni.exception;

import static java.lang.String.format;

public class ReservationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 244025171917935864L;

    public ReservationNotFoundException(String id) {
        super(format("Reservation not found for id %s.", id));
    }
}
