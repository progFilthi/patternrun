package com.patternrun.progress.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The streak, plus the last fortnight so the widget can draw it.
 *
 * The trail is included so the client does not have to invent a history, and so a streak the
 * learner cannot see is not one they have to trust.
 */
public record StreakResponse(
        int current,
        int longest,
        boolean trainedToday,
        LocalDate today,
        List<LocalDate> metDays) {
}
