package com.patternrun.attempt.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Finishes a session.
 *
 * Everything here is an observation. The correctness flags that result from it are derived by
 * the server, which is what makes them worth storing.
 *
 * {@code durationMs} is sanity checked rather than trusted: anything under a few seconds or over
 * a couple of hours is a broken client clock, not a training session, and accepting it would
 * corrupt the speed axis permanently.
 *
 * There is no breakdown flag here on purpose. Whether the learner read the problem correctly is
 * derived from the answers they submitted to the breakdown step, which the server stored and can
 * judge itself. Accepting the claim instead would have been worth twenty XP for one boolean in a
 * request body.
 */
public record CompleteAttemptRequest(
        @Pattern(regexp = "O\\(.*\\)", message = "must be a complexity like O(n)") String complexityTime,
        @Pattern(regexp = "O\\(.*\\)", message = "must be a complexity like O(n)") String complexitySpace,
        // Generous on purpose. Plausibility is CorrectnessChecking's job, which drops an
        // implausible value rather than rejecting the whole session for a bad clock.
        @Min(0) @Max(86_400_000) Long durationMs,
        @Pattern(regexp = "JAVA|PYTHON") String language,
        List<SubmittedPrediction> predictions) {

    public record SubmittedPrediction(int stepOrder, int chosenIndex) {
    }
}
