package com.patternrun.progress;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Streak and quest arithmetic, with no database and no clock.
 *
 * The rule that decides how this feels is in {@link #currentStreak}: a streak survives until the
 * end of the day after it was last met. Breaking it at midnight would mean opening the app
 * before training and seeing a streak you did not actually lose yet, which is exactly the kind
 * of small punishment that makes people stop.
 */
public final class DailyProgressScoring {

    /** The daily goal is one problem or ten minutes. Sustainable on purpose (section 34). */
    public static final int DAILY_GOAL_MINUTES = 10;

    private DailyProgressScoring() {
    }

    public static boolean meetsGoal(int problemsCompleted, int activeMinutes) {
        return problemsCompleted >= 1 || activeMinutes >= DAILY_GOAL_MINUTES;
    }

    /**
     * Consecutive days met, counting back from today.
     *
     * Today not being met yet does not break anything, because the day is not over. Only a gap of
     * two whole days does, which means one missed day is forgiven rather than fatal.
     */
    public static int currentStreak(Collection<LocalDate> goalMetDays, LocalDate today) {
        if (goalMetDays == null || goalMetDays.isEmpty()) {
            return 0;
        }
        Set<LocalDate> days = new TreeSet<>(goalMetDays);

        LocalDate cursor = days.contains(today)
                ? today
                : days.contains(today.minusDays(1)) ? today.minusDays(1) : null;
        if (cursor == null) {
            return 0;
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    public static int longestStreak(Collection<LocalDate> goalMetDays) {
        if (goalMetDays == null || goalMetDays.isEmpty()) {
            return 0;
        }
        List<LocalDate> sorted = new TreeSet<>(goalMetDays).stream().toList();
        int longest = 1;
        int running = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                running++;
            } else {
                running = 1;
            }
            longest = Math.max(longest, running);
        }
        return longest;
    }

    /** True when today is already met, so the UI can say so instead of implying otherwise. */
    public static boolean isActiveToday(Collection<LocalDate> goalMetDays, LocalDate today) {
        return goalMetDays != null && goalMetDays.contains(today);
    }

    /**
     * The four daily quests from section 35.
     *
     * Every one of them is optional, so the quest is a bonus rather than a second obligation on
     * top of the daily goal. Nothing is locked behind it.
     */
    public static boolean questCompleted(DailyCounts counts) {
        return counts.patternsIdentified() >= 3
                && counts.problemsCompleted() >= 1
                && counts.mistakesReviewed() >= 1
                && counts.speedrunsCompleted() >= 1;
    }

    public static int questItemsCompleted(DailyCounts counts) {
        return (counts.patternsIdentified() >= 3 ? 1 : 0)
                + (counts.problemsCompleted() >= 1 ? 1 : 0)
                + (counts.mistakesReviewed() >= 1 ? 1 : 0)
                + (counts.speedrunsCompleted() >= 1 ? 1 : 0);
    }

    public record DailyCounts(
            int patternsIdentified,
            int problemsCompleted,
            int mistakesReviewed,
            int speedrunsCompleted) {
    }
}