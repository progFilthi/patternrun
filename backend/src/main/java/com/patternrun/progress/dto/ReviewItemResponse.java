package com.patternrun.progress.dto;

import java.time.Instant;

/**
 * A mistake that is due for review.
 *
 * The lesson travels with it, because a review that does not show why it was wrong is a
 * repetition rather than a correction.
 */
public record ReviewItemResponse(
        String mistakeId,
        String problemSlug,
        String problemTitle,
        String category,
        String description,
        String lesson,
        int reviewCount,
        int intervalDays,
        Instant dueAt,
        long attemptsSince) {
}
