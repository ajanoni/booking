package com.ajanoni.exception;

public class LockAcquireException extends RuntimeException {

    public LockAcquireException(String message) {
        super(message);
    }
}
