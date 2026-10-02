package com.patternrun.progress;

/**
 * Why XP was paid (README section 32). The values are the only XP table in the system;
 * {@code XpService} maps each one to a fixed amount.
 */
public enum XpReason {
    PATTERN_IDENTIFIED(10),
    PREDICTION_CORRECT(5),
    PROBLEM_COMPLETED(50),
    /**
     * The extra for a run of consecutive correct solves.
     *
     * Its own reason rather than a second PROBLEM_COMPLETED row, so that counting completion
     * awards still counts completions. Keyed per attempt, so it is idempotent on retry.
     */
    COMBO_BONUS(0),
    NO_HINTS(20),
    CORRECT_COMPLEXITY(10),
    /** From the problem breakdown step, where the learner read the statement back. */
    PROBLEM_BREAKDOWN(20),
    /** Monotonic guard rather than an idempotency key: repeatable as the record improves. */
    SPEEDRUN_PB(25),
    BOSS_DEFEATED(100),
    REVIEW_COMPLETED(20),
    DAILY_QUEST(100),
    /** Resolving an open mistake. Worth more than grinding a fresh problem (section 104). */
    COMEBACK(20);

    private final int xp;

    XpReason(int xp) {
        this.xp = xp;
    }

    public int xp() {
        return xp;
    }
}
