package com.patternrun.progress;

import com.patternrun.attempt.AttemptEntity;
import com.patternrun.attempt.AttemptMode;
import com.patternrun.attempt.AttemptStatus;
import com.patternrun.attempt.PredictionRecord;
import com.patternrun.pattern.PatternEntity;
import com.patternrun.problem.Difficulty;
import com.patternrun.problem.ProblemEntity;
import java.math.BigDecimal;
import java.util.List;

/**
 * Attempt fixtures for the scoring tests.
 *
 * Built through the setters rather than a database, because these tests are about arithmetic and
 * the degenerate cases are the interesting ones: no timing, no predictions, no attempts at all.
 */
final class AttemptFixtures {

    private AttemptFixtures() {
    }

    static PatternEntity pattern() {
        PatternEntity pattern = new PatternEntity();
        pattern.setSlug("hashing");
        return pattern;
    }

    static ProblemEntity problem(Difficulty difficulty) {
        ProblemEntity problem = new ProblemEntity();
        problem.setSlug("two-sum");
        problem.setDifficulty(difficulty);
        problem.setPrimaryPattern(pattern());
        return problem;
    }

    static AttemptEntity attempt() {
        return attempt(Difficulty.EASY);
    }

    static AttemptEntity attempt(Difficulty difficulty) {
        AttemptEntity attempt = new AttemptEntity();
        attempt.setProblem(problem(difficulty));
        attempt.setPattern(pattern());
        attempt.setMode(AttemptMode.STANDARD);
        attempt.setStatus(AttemptStatus.COMPLETED);
        return attempt;
    }

    /** A completed attempt that got everything right. */
    static AttemptEntity clean(Difficulty difficulty, long durationMs) {
        AttemptEntity attempt = attempt(difficulty);
        attempt.setPatternCorrect(true);
        attempt.setComplexityCorrect(true);
        attempt.setBreakdownCorrect(true);
        attempt.setDurationMs(durationMs);
        attempt.setPredictions(List.of(
                new PredictionRecord(1, 0, true),
                new PredictionRecord(2, 0, true)));
        return attempt;
    }

    /** Solved, but the pattern was never identified and the complexity was wrong. */
    static AttemptEntity poor(Difficulty difficulty) {
        AttemptEntity attempt = attempt(difficulty);
        attempt.setPatternCorrect(false);
        attempt.setComplexityCorrect(false);
        attempt.setBreakdownCorrect(false);
        attempt.setDurationMs(600_000L);
        return attempt;
    }

    /** History recovered from the browser: no timing was ever recorded. */
    static AttemptEntity imported(boolean patternCorrect, boolean complexityCorrect, int hintsUsed) {
        AttemptEntity attempt = attempt(Difficulty.EASY);
        attempt.setSource(com.patternrun.attempt.AttemptSource.IMPORT);
        attempt.setPatternCorrect(patternCorrect);
        attempt.setComplexityCorrect(complexityCorrect);
        attempt.setHintsUsed(hintsUsed);
        attempt.setDurationMs(null);
        return attempt;
    }

    static AttemptEntity abandoned() {
        AttemptEntity attempt = attempt(Difficulty.EASY);
        attempt.setStatus(AttemptStatus.ABANDONED);
        attempt.setPatternCorrect(true);
        return attempt;
    }

    static BigDecimal overallOf(
            BigDecimal recognition,
            BigDecimal correctness,
            BigDecimal speed,
            BigDecimal explanation,
            BigDecimal retention) {
        return MasteryScoring.overall(
                MasteryScoring.axesOf(recognition, correctness, speed, explanation, retention));
    }
}