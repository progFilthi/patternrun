package com.patternrun.attempt;

/**
 * Whether the learner was right, decided server-side.
 *
 * Everything in the completion request is an observation: which pattern was named, which option
 * was picked, which complexity was claimed. This class turns those into judgements by comparing
 * them against content the server already holds, and it is deliberately a pure function of its
 * arguments so every rule here is testable without a database.
 *
 * Why bother, when the client could just send a boolean? Because the answer to each predict the
 * move question is already inside the animation payload the browser received. A client that
 * reports its own correctness is asserting something the server can simply look up, so the
 * client is not asked to.
 */
public final class CorrectnessChecking {

    /** Below this a session was not really attempted; above it the clock is wrong. */
    static final long MIN_PLAUSIBLE_DURATION_MS = 5_000;
    static final long MAX_PLAUSIBLE_DURATION_MS = 2 * 60 * 60 * 1000L;

    private CorrectnessChecking() {
    }

    /**
     * "O(n²)" and "O(n^2)" are the same answer.
     *
     * Both the superscript and the caret are folded to a canonical form, and a missing answer is
     * never right.
     *
     * This matters more than it looks. The seed stores complexity as a caret ("O(n^2)") while
     * the learner's options are written with a superscript ("O(n²)"), so stripping only the
     * caret left the two forms unequal and made the correct answer unreachable on any problem
     * whose complexity is quadratic. The bug cost those learners the complexity award and the
     * correctness half of their mastery, and told them they were wrong when they were not.
     */
    public static boolean complexityMatches(String choice, String expected) {
        if (choice == null || expected == null) {
            return false;
        }
        return normaliseComplexity(choice).equals(normaliseComplexity(expected));
    }

    /**
     * Both halves have to be right.
     *
     * The step asks for time and space separately, and half-right is not right: a learner who
     * says O(n) time and O(1) space has not reasoned about the algorithm.
     */
    public static boolean complexityCorrect(
            String timeChoice, String spaceChoice, String expectedTime, String expectedSpace) {
        return complexityMatches(timeChoice, expectedTime)
                && complexityMatches(spaceChoice, expectedSpace);
    }

    /** Both halves have to be wrong for the answer to count as incorrect, for the same reason. */
    public static boolean complexityIncorrect(
            String timeChoice, String spaceChoice, String expectedTime, String expectedSpace) {
        return !complexityCorrect(timeChoice, spaceChoice, expectedTime, expectedSpace);
    }

    /** Whether a prediction matched the answer the animation step already holds. */
    public static boolean predictionCorrect(int chosenIndex, Integer answerIndex) {
        return answerIndex != null && chosenIndex == answerIndex;
    }

    public static String normaliseComplexity(String value) {
        return value.trim()
                .toLowerCase()
                .replace('²', '2')
                .replace('³', '3')
                .replace("^", "");
    }

    /**
     * Whether a reported duration could be real.
     *
     * Two seconds and four hours are both clock problems. A rejected duration is stored as null
     * rather than as zero, so the speed axis stays unmeasured instead of being poisoned for
     * good by one bad request.
     */
    public static boolean isPlausibleDuration(Long durationMs) {
        if (durationMs == null) {
            return true;
        }
        return durationMs >= MIN_PLAUSIBLE_DURATION_MS && durationMs <= MAX_PLAUSIBLE_DURATION_MS;
    }

    public static Long sanitiseDuration(Long durationMs) {
        return isPlausibleDuration(durationMs) ? durationMs : null;
    }

    /** Whether the reported pattern slug is the problem's primary pattern. */
    public static boolean patternCorrect(String guess, String primaryPatternSlug) {
        return guess != null && primaryPatternSlug != null
                && guess.trim().equalsIgnoreCase(primaryPatternSlug.trim());
    }
}