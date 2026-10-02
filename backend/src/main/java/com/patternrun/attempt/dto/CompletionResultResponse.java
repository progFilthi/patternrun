package com.patternrun.attempt.dto;

import java.util.List;
import java.util.UUID;

/**
 * What one finished session earned.
 *
 * The award list is the point. A single "you earned 60 XP" number hides why, and a number
 * without its reasons is not motivating, it is just a score. Returning the breakdown makes
 * README section 32's "reward useful behaviour" legible instead of arbitrary.
 *
 * {@code provisional} is true when the grade had to leave parts of the rubric unevidenced,
 * which happens for an attempt with no timing. It is reported rather than hidden, because a
 * score that silently omits half its rubric misrepresents the learner.
 */
public record CompletionResultResponse(
        UUID attemptId,
        String problemSlug,
        String grade,
        boolean provisional,
        boolean patternCorrect,
        boolean complexityCorrect,
        boolean breakdownCorrect,
        int predictionsAnswered,
        int predictionsCorrect,
        int hintsUsed,
        Long durationMs,
        int totalXpAwarded,
        int combo,
        List<XpAwardResponse> awards,
        String patternSlug,
        Double patternMastery,
        boolean personalBest,
        List<String> mistakesResolved) {

    /** One line of the ledger, as it should appear on screen. */
    public record XpAwardResponse(String reason, String label, int xp, boolean alreadyEarned) {
    }
}
