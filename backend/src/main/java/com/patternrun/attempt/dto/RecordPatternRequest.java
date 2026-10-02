package com.patternrun.attempt.dto;

import jakarta.validation.constraints.Pattern;

/**
 * The pattern the learner committed to, before being told the answer.
 *
 * Only the slug travels. Whether it was right is worked out against the problem's primary
 * pattern, because a client reporting its own guess as correct would be asserting something the
 * server already holds.
 */
public record RecordPatternRequest(
        @Pattern(regexp = "[a-z0-9-]+", message = "must be a pattern slug") String patternSlug) {
}
