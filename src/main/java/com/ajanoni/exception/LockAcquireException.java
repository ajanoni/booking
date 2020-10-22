package com.ajanoni.exception;

public class LockAcquireException extends RuntimeException {

    private static final long serialVersionUID = 5377509189110150651L;

    public LockAcquireException(String message) {
        super(message);
    }
}
