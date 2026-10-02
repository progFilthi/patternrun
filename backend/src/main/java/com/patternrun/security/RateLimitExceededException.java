package com.patternrun.security;

/** Too many writes from one caller. Rendered as 429 (README section 90). */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
