package com.patternrun.progress.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * One call that fills the dashboard.
 *
 * Deliberately a single endpoint rather than six. The dashboard, the header and the streak
 * widget all need these numbers at once, and six round trips to render one screen is six
 * chances to show a half-painted page.
 */
public record ProgressSummaryResponse(
        long totalXp,
        int level,
        String levelName,
        double levelProgress,
        int streak,
        int longestStreak,
        boolean trainedToday,
        DailyProgressResponse today,
        List<PatternMasteryResponse> patterns,
        long problemsCompleted,
        long openMistakes) {

    /** Today's row, or null when the learner has not trained yet today. */
    public record DailyProgressResponse(
            LocalDate day,
            int xpEarned,
            int problemsCompleted,
            int patternsIdentified,
            int mistakesReviewed,
            int speedrunsCompleted,
            int activeMinutes,
            boolean goalMet,
            boolean questCompleted,
            int questItemsCompleted) {
    }
}
