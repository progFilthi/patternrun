package com.patternrun.content;

/** Raised when seed content is missing, unparsable or violates the content contract. */
public class SeedContentException extends RuntimeException {

    public SeedContentException(String message) {
        super(message);
    }

    public SeedContentException(String message, Throwable cause) {
        super(message, cause);
    }
}