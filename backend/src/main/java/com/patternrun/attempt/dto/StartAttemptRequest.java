package com.patternrun.attempt.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Opens a training session.
 *
 * The problem is named by slug because that is what the frontend routes on, and the server
 * resolves it. A client cannot invent a problem id it was not given.
 */
public record StartAttemptRequest(
        @Pattern(regexp = "[a-z0-9-]+", message = "must be a problem slug") String problemSlug,
        @Pattern(regexp = "STANDARD|SPEEDRUN|BOSS", message = "must be STANDARD, SPEEDRUN or BOSS")
        String mode) {
}
