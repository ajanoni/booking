package com.ajanoni.exception;

import java.util.List;

public class ReservationRequestException extends RuntimeException {

    private final List<String> messages;

    public ReservationRequestException(String message) {
        super("Error validating reservation request.");
        messages = List.of(message);
    }

    public ReservationRequestException(List<String> messages) {
        super("Error validating reservation request.");
        this.messages = messages;
    }

    public List<String> getMessages() {
        return List.copyOf(messages);
    }
}
