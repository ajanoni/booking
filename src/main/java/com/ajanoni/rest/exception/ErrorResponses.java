package com.ajanoni.rest.exception;

import java.util.List;
import java.util.stream.Collectors;

public final class ErrorResponses {

    private final List<ErrorMessage> errors;

    private ErrorResponses(List<ErrorMessage> errors) {
        this.errors = List.copyOf(errors);
    }

    public static ErrorResponses of(List<String> messages) {
        List<ErrorMessage> messageList =
                messages.stream().map(msg -> new ErrorMessage(msg)).collect(Collectors.toList());
        return new ErrorResponses(messageList);
    }

    public static ErrorResponses of(String message) {
        return new ErrorResponses(List.of(new ErrorMessage(message)));
    }

    public List<ErrorMessage> getErrors() {
        return errors;
    }

    private static class ErrorMessage {

        private final String message;

        public ErrorMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
