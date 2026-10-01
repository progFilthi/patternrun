package com.patternrun.common;

import java.time.Instant;

/**
 * Error body returned to clients. Never contains stack traces or SQL details
 * (README section 90).
 */
public record ApiErrorResponse(int status, String error, String message, String path, Instant timestamp) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(status, error, message, path, Instant.now());
    }
}