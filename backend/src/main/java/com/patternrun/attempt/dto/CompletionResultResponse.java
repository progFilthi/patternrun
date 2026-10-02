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
 *
 * <p>The three Phase 4 fields describe the coding stage and feed nothing into the grade yet.
 * {@code codeAccepted} is the server's verdict from running the code against the hidden
 * evaluation set, {@code codeOutcome} is how the last submission ended, and
 * {@code solvedIndependently} additionally requires that no hint was revealed and the reference
 * solution was not shown. They are reported because a learner who just wrote a passing solution
 * deserves to be told so, and because gating the grade on them is a product decision that should
 * be made with the data visible rather than smuggled in here.
 *
 * @param codeOutcome null when no code has been evaluated, which means "not measured" rather
 *     than "failed". Present when the last submission was rejected.
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
        List<String> mistakesResolved,
        String codeOutcome,
        boolean codeAccepted,
        boolean solvedIndependently) {

    /** One line of the ledger, as it should appear on screen. */
    public record XpAwardResponse(String reason, String label, int xp, boolean alreadyEarned) {
    }
}
