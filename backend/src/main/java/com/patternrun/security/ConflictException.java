package com.patternrun.security;

/** The request is well formed but conflicts with existing state. Rendered as 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
