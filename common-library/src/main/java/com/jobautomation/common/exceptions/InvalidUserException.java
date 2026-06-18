package com.jobautomation.common.exceptions;

/**
 * Exception thrown when a user cannot be found or credentials are invalid.
 * Renamed from the original InvalideUserExeption (typo fixed).
 */
public class InvalidUserException extends RuntimeException {

    public InvalidUserException(String message) {
        super(message);
    }

    public InvalidUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
