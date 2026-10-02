package com.patternrun.progress;

import java.util.List;

/**
 * Levels and combo, the two pieces the spec names but never quantifies.
 *
 * Section 102 says levels are cosmetic and must not imply that a level guarantees an interview
 * outcome, so they are a pure function of lifetime XP with no side effects and nothing unlocked
 * behind them. Combo is a per-day multiplier on one award, capped so it cannot outgrow the
 * behaviour it is meant to encourage.
 */
public final class LevelScoring {

    /** Cosmetic titles only (README section 102). */
    private static final List<Level> LEVELS = List.of(
            new Level(0, "Rookie"),
            new Level(150, "Pattern Scout"),
            new Level(400, "Problem Solver"),
            new Level(800, "Algorithm Builder"),
            new Level(1400, "Speedrunner"),
            new Level(2200, "Interview Ready"),
            new Level(3200, "Pattern Master"));

    /** A four-problem run already takes a long time, so the multiplier stops there. */
    public static final int MAX_COMBO = 4;

    private LevelScoring() {
    }

    public static int levelNumber(long totalXp) {
        int level = 1;
        for (int i = 0; i < LEVELS.size(); i++) {
            if (totalXp >= LEVELS.get(i).minimumXp()) {
                level = i + 1;
            }
        }
        return level;
    }

    public static String levelName(long totalXp) {
        return LEVELS.get(levelNumber(totalXp) - 1).title();
    }

    /** Progress through the current level, as 0-1. */
    public static double levelProgress(long totalXp) {
        int level = levelNumber(totalXp);
        if (level >= LEVELS.size()) {
            return 1;
        }
        Level current = LEVELS.get(level - 1);
        Level next = LEVELS.get(level);
        long span = next.minimumXp() - current.minimumXp();
        if (span <= 0) {
            return 1;
        }
        return Math.min(1.0, (double) (totalXp - current.minimumXp()) / span);
    }

    /**
     * The multiplier for a completion, given how many correct problems have already been finished
     * today.
     *
     * Applied to the completion award only. It never touches mastery and never touches hints or
     * breakdown awards, because a multiplier that rewards speed must not also reward skipping the
     * parts that make the learning stick (section 96).
     */
    public static double comboMultiplier(int correctCompletionsToday) {
        return Math.min(Math.max(correctCompletionsToday, 1), MAX_COMBO);
    }

    /**
     * Break the combo by doing something, in the spirit of section 33.
     *
     * Returns the new run length. Passing 0 for a break event is how an intentional skip or a
     * repeated failure resets it.
     */
    public static int nextComboAfter(boolean broke, int currentRun) {
        return broke ? 0 : currentRun + 1;
    }

    private record Level(long minimumXp, String title) {
    }
}