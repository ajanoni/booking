package com.ajanoni.exception;

import java.util.List;

public class ReservationRequestException extends RuntimeException {

    private static final long serialVersionUID = -153319434432402663L;
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
