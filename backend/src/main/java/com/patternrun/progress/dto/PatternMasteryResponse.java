package com.patternrun.progress.dto;

import java.math.BigDecimal;

/**
 * A learner's standing in one pattern.
 *
 * The axis fields are nullable all the way to the client, on purpose. A learner who has never
 * been reviewed has no retention score, and sending 0 would tell them they are bad at something
 * that was never measured. The client shows an axis as "not measured yet" instead.
 */
public record PatternMasteryResponse(
        String patternSlug,
        String patternName,
        int difficultyOrder,
        BigDecimal recognition,
        BigDecimal correctness,
        BigDecimal explanation,
        BigDecimal speed,
        BigDecimal retention,
        BigDecimal overall,
        int attemptsCount) {
}
