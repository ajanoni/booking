package com.ajanoni.exception;

public class ReservationConflictException extends RuntimeException {

    private static final long serialVersionUID = -6643912891824868731L;

    public ReservationConflictException(String message) {
        super(message);
    }
}
