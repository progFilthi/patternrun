package com.patternrun.attempt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * What the learner chose for each breakdown prompt.
 *
 * Keys and chosen indices only. No correctness claim is accepted, because the server holds the
 * answer key and can simply look it up.
 */
public record SubmitBreakdownRequest(
        @NotEmpty List<@Valid Answer> answers) {

    public record Answer(
            @Pattern(regexp = "[a-z0-9-]+", message = "must be a prompt key") String key,
            @Min(0) int chosenIndex) {
    }
}
