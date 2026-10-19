package com.example.exceptions.exception;

/**
 * Thrown when a transfer request breaks a transfer rule (e.g. same source and target).
 *
 * <p>UNCHECKED: this is invalid input from the client. Validation catches it before
 * any money moves, and the global handler maps it to HTTP 400.
 */
public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
