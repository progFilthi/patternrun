package com.patternrun.progress.dto;

import com.patternrun.mistake.MistakeReviewService;
import java.time.Instant;
import java.util.UUID;

/**
 * What reviewing one mistake did.
 *
 * The interval and the next due date are returned so the client can show when this will come back
 * rather than making the learner re-open the queue to find out. Both are server-owned and computed
 * from {@link com.patternrun.mistake.MistakeEntity#markReviewed}.
 */
public record ReviewOutcomeResponse(
        UUID mistakeId,
        String problemSlug,
        String category,
        boolean correct,
        int reviewCount,
        int intervalDays,
        Instant nextDueAt,
        int xpAwarded,
        /** True when this review earned nothing because it had already been paid for. */
        boolean alreadyEarned) {

    public static ReviewOutcomeResponse of(MistakeReviewService.ReviewOutcome outcome) {
        return new ReviewOutcomeResponse(
                outcome.mistakeId(),
                outcome.problemSlug(),
                outcome.category(),
                outcome.correct(),
                outcome.reviewCount(),
                outcome.intervalDays(),
                outcome.nextDueAt(),
                outcome.xpAwarded(),
                outcome.alreadyEarned());
    }
}
