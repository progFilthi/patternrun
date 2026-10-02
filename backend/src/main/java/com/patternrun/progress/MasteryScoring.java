package com.patternrun.progress;

import com.patternrun.attempt.AttemptEntity;
import com.patternrun.attempt.AttemptStatus;
import com.patternrun.problem.Difficulty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mastery and grade arithmetic, with no database and no clock.
 *
 * These are the numbers a learner sees, and every one of them is a pure function of stored
 * attempts. Keeping them here rather than inline in a service is what makes them testable at
 * all: the interesting cases are the degenerate ones, an attempt with no timing, a prediction
 * set that was never answered, and they are all reachable without a database.
 *
 * Two rules run through everything below.
 *
 * <ul>
 *   <li><b>Unknown is not zero.</b> A learner with no timing has no speed score, not a speed
 *       score of zero. Averaging an unmeasured axis in as a failure would tell someone who has
 *       simply never been timed that they are slow.
 *   <li><b>Rates are averaged, counts are not.</b> Solving ten problems badly is not mastery,
 *       so nothing here can reach a high value by volume alone.
 * </ul>
 */
public final class MasteryScoring {

    /** Section 37's weights. They sum to 1, but the denominator is recomputed per learner. */
    public static final double WEIGHT_RECOGNITION = 0.25;
    public static final double WEIGHT_CORRECTNESS = 0.30;
    public static final double WEIGHT_EXPLANATION = 0.15;
    public static final double WEIGHT_SPEED = 0.15;
    public static final double WEIGHT_RETENTION = 0.15;

    /** Within an attempt, how much complexity and prediction accuracy are each worth. */
    private static final double COMPLEXITY_SHARE = 0.4;
    private static final double PREDICTION_SHARE = 0.6;

    /** Expected duration per difficulty, in seconds. Provisional: see readPerformanceSeconds. */
    private static final double EXPECTED_SECONDS_EASY = 300;
    private static final double EXPECTED_SECONDS_MEDIUM = 600;
    private static final double EXPECTED_SECONDS_HARD = 900;

    /** Section 96's rubric, and the share of it a Phase 3 attempt can actually evidence. */
    private static final double RUBRIC_PATTERN = 20;
    private static final double RUBRIC_COMPLEXITY = 10;
    private static final double RUBRIC_EXPLANATION = 10;
    private static final double RUBRIC_SPEED = 10;

    private MasteryScoring() {
    }

    /**
     * How often the learner named the right pattern before seeing the answer.
     *
     * This is the axis that cannot be inflated by grinding: repeating a problem you have solved
     * adds attempts that all count, but only a correct read improves the rate.
     */
    public static BigDecimal recognition(Collection<AttemptEntity> attempts) {
        // Filtered here rather than trusted to the caller: an abandoned session is not evidence
        // of anything, and a caller forgetting to filter would quietly inflate recognition.
        var considered = completedOnly(attempts);
        long size = considered.size();
        if (size == 0) {
            return null;
        }
        long correct = considered.stream().filter(AttemptEntity::isPatternCorrect).count();
        return percent(correct, size);
    }

    /**
     * Did the learner get the substance right: the complexity they claimed, and whether their
     * predictions held.
     *
     * Difficulty weights each attempt, because getting a hard problem right is stronger evidence
     * than getting an easy one right. The result is clamped rather than allowed to exceed 100,
     * since a weighted mean of 0-100 rates can.
     */
    public static BigDecimal correctness(Collection<AttemptEntity> attempts) {
        var completed = completedOnly(attempts);
        if (completed.isEmpty()) {
            return null;
        }
        double weighted = 0;
        double weightTotal = 0;
        for (AttemptEntity attempt : completed) {
            double rate = attemptCorrectnessRate(attempt);
            if (rate < 0) {
                continue;
            }
            weighted += rate * difficultyWeight(attempt);
            weightTotal += difficultyWeight(attempt);
        }
        if (weightTotal == 0) {
            return null;
        }
        return scaled(clamp(weighted / weightTotal * 100));
    }

    /**
     * How fast, relative to what the difficulty suggests.
     *
     * A ratio rather than a curve: beating the expected time gives full marks and taking twice
     * as long gives half. Only attempts that carry a duration are considered, and the axis is
     * null when none do.
     */
    public static BigDecimal speed(Collection<AttemptEntity> attempts) {
        var completed = completedOnly(attempts);
        double total = 0;
        int counted = 0;
        for (AttemptEntity attempt : completed) {
            if (attempt.getDurationMs() == null) {
                continue;
            }
            double expected = expectedSeconds(attempt);
            double actual = attempt.getDurationMs() / 1000.0;
            if (actual <= 0) {
                continue;
            }
            // Not clamped per attempt: clamping here would make 2x-under and 5x-under
            // identical, which flattens the axis to a near-binary "fast or not".
            total += expected / actual * 100;
            counted++;
        }
        return counted == 0 ? null : scaled(clamp(total / counted));
    }

    /** The §97 label for a single attempt. Descriptive only, never a hiring claim. */
    public static String grade(AttemptEntity attempt) {
        return gradeFor(scoreOutOf100(attempt));
    }

    public static String gradeFor(double score) {
        if (score >= 90) {
            return "EXCELLENT";
        }
        if (score >= 75) {
            return "STRONG";
        }
        if (score >= 60) {
            return "DEVELOPING";
        }
        return "NEEDS_REVIEW";
    }

    /**
     * One attempt against §96's 100-point rubric, renormalised over the parts it evidences.
     *
     * Implementation and tests are 50 points that Phase 3 cannot score, so an attempt that
     * answered only the pattern, complexity and breakdown questions is graded out of 50 and
     * rescaled, with {@code provisional} telling the client it was.
     */
    public static double scoreOutOf100(AttemptEntity attempt) {
        double earned = 0;
        double available = 0;

        earned += RUBRIC_PATTERN * (attempt.isPatternCorrect() ? 1 : 0);
        available += RUBRIC_PATTERN;

        earned += RUBRIC_COMPLEXITY * (attempt.isComplexityCorrect() ? 1 : 0);
        available += RUBRIC_COMPLEXITY;

        earned += RUBRIC_EXPLANATION * (attempt.isBreakdownCorrect() ? 1 : 0);
        available += RUBRIC_EXPLANATION;

        if (attempt.getDurationMs() != null && attempt.getDurationMs() > 0) {
            double expected = expectedSeconds(attempt);
            double actual = attempt.getDurationMs() / 1000.0;
            earned += RUBRIC_SPEED * clamp(expected / actual * 100) / 100;
            available += RUBRIC_SPEED;
        }

        return available == 0 ? 0 : clamp(earned / available * 100);
    }

    /** Whether a grading pass had to leave parts of the rubric unevidenced. */
    public static boolean isProvisionalGrade(AttemptEntity attempt) {
        return attempt.getDurationMs() == null || attempt.getDurationMs() <= 0;
    }

    /**
     * §37's weighted average over the axes that exist.
     *
     * The spec's formula sums to 1.00 across five axes, but Phase 3 can only measure three. Summed
     * literally it would cap every learner's mastery near 70 for the whole of this phase and read
     * as failure. Dividing by the weight that was actually measured keeps the number meaningful
     * now, and an axis that starts being measured later folds in with no migration.
     */
    public static BigDecimal overall(Map<String, BigDecimal> axes) {
        double weighted = 0;
        double available = 0;
        weighted += WEIGHT_RECOGNITION * value(axes, "recognition", WEIGHT_RECOGNITION);
        available += present(axes, "recognition") ? WEIGHT_RECOGNITION : 0;
        weighted += WEIGHT_CORRECTNESS * value(axes, "correctness", WEIGHT_CORRECTNESS);
        available += present(axes, "correctness") ? WEIGHT_CORRECTNESS : 0;
        weighted += WEIGHT_EXPLANATION * value(axes, "explanation", WEIGHT_EXPLANATION);
        available += present(axes, "explanation") ? WEIGHT_EXPLANATION : 0;
        weighted += WEIGHT_SPEED * value(axes, "speed", WEIGHT_SPEED);
        available += present(axes, "speed") ? WEIGHT_SPEED : 0;
        weighted += WEIGHT_RETENTION * value(axes, "retention", WEIGHT_RETENTION);
        available += present(axes, "retention") ? WEIGHT_RETENTION : 0;

        return available == 0 ? null : scaled(weighted / available);
    }

    /** Convenience for the common shape of a full axis map. */
    public static Map<String, BigDecimal> axesOf(
            BigDecimal recognition,
            BigDecimal correctness,
            BigDecimal speed,
            BigDecimal explanation,
            BigDecimal retention) {
        Map<String, BigDecimal> axes = new LinkedHashMap<>();
        axes.put("recognition", recognition);
        axes.put("correctness", correctness);
        axes.put("speed", speed);
        axes.put("explanation", explanation);
        axes.put("retention", retention);
        return axes;
    }

    /** Completed attempts only. Abandoned and in-flight sessions are not evidence of anything. */
    public static java.util.List<AttemptEntity> completedOnly(Collection<AttemptEntity> attempts) {
        return attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .toList();
    }

    /**
     * A single attempt's correctness rate, renormalised over the signals it carries.
     *
     * @return 0-1, or -1 when the attempt evidences neither signal
     */
    private static double attemptCorrectnessRate(AttemptEntity attempt) {
        double earned = COMPLEXITY_SHARE * (attempt.isComplexityCorrect() ? 1 : 0);
        double available = COMPLEXITY_SHARE;
        if (!attempt.getPredictions().isEmpty()) {
            long correct = attempt.getPredictions().stream()
                    .filter(prediction -> prediction.correct())
                    .count();
            earned += PREDICTION_SHARE * ((double) correct / attempt.getPredictions().size());
            available += PREDICTION_SHARE;
        }
        return earned / available;
    }

    static double difficultyWeight(AttemptEntity attempt) {
        return difficultyWeight(
                attempt.getProblem() == null ? null : attempt.getProblem().getDifficulty());
    }

    static double difficultyWeight(Difficulty difficulty) {
        if (difficulty == Difficulty.HARD) {
            return 1.5;
        }
        if (difficulty == Difficulty.MEDIUM) {
            return 1.25;
        }
        return 1.0;
    }

    static double expectedSeconds(AttemptEntity attempt) {
        return expectedSeconds(
                attempt.getProblem() == null ? null : attempt.getProblem().getDifficulty());
    }

    /**
     * Provisional baseline. Twenty seeded problems do not justify per-problem expected times in
     * content yet; difficulty is a defensible stand-in and can move into the seed JSON later
     * without a schema change.
     */
    static double expectedSeconds(Difficulty difficulty) {
        if (difficulty == Difficulty.HARD) {
            return EXPECTED_SECONDS_HARD;
        }
        if (difficulty == Difficulty.MEDIUM) {
            return EXPECTED_SECONDS_MEDIUM;
        }
        return EXPECTED_SECONDS_EASY;
    }

    private static boolean present(Map<String, BigDecimal> axes, String key) {
        return axes.get(key) != null;
    }

    private static double value(Map<String, BigDecimal> axes, String key, double weight) {
        BigDecimal score = axes.get(key);
        return score == null ? 0 : score.doubleValue();
    }

    private static BigDecimal percent(long part, long total) {
        return scaled((double) part / total * 100);
    }

    private static BigDecimal scaled(double value) {
        return BigDecimal.valueOf(clamp(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private static double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }
}