package com.patternrun.attempt.dto;

import com.patternrun.attempt.AttemptMode;
import com.patternrun.attempt.AttemptStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * An attempt as the client needs it mid-session: enough to render the phase, not enough to
 * reconstruct the session.
 */
public record AttemptResponse(
        UUID id,
        String problemSlug,
        AttemptMode mode,
        AttemptStatus status,
        int hintsUsed,
        boolean patternCorrect,
        int predictionsAnswered,
        int predictionsCorrect,
        boolean complexityCorrect,
        boolean breakdownCorrect,
        Long durationMs,
        Instant completedAt) {

    public static AttemptResponse of(com.patternrun.attempt.AttemptEntity attempt) {
        int correct = (int) attempt.getPredictions().stream()
                .filter(prediction -> prediction.correct())
                .count();
        return new AttemptResponse(
                attempt.getId(),
                attempt.getProblem().getSlug(),
                attempt.getMode(),
                attempt.getStatus(),
                attempt.getHintsUsed(),
                attempt.isPatternCorrect(),
                attempt.getPredictions().size(),
                correct,
                attempt.isComplexityCorrect(),
                attempt.isBreakdownCorrect(),
                attempt.getDurationMs(),
                attempt.getCompletedAt());
    }
}
