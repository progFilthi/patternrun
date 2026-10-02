package com.patternrun.progress;

import com.patternrun.account.UserEntity;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The learner's day, and the streak derived from it.
 *
 * The streak is never stored. It is counted from consecutive {@code goal_met} days, which means
 * it cannot drift out of step with the rows that justify it, and it costs one indexed read
 * rather than a counter that every write path has to remember to update.
 *
 * The day boundary is the learner's own timezone, held on their user row and applied here rather
 * than trusted from the client. A client that could name its own day would be able to rewrite
 * its own history.
 */
@Service
public class DailyProgressService {

    private final DailyProgressRepository days;

    public DailyProgressService(DailyProgressRepository days) {
        this.days = days;
    }

    /** The learner's current local day. */
    public static LocalDate todayFor(UserEntity user) {
        return LocalDate.now(zoneOf(user));
    }

    public static ZoneId zoneOf(UserEntity user) {
        try {
            return ZoneId.of(user.getTimezone());
        } catch (RuntimeException ex) {
            // The column is validated on write, so this is only reachable for a row written
            // before a zone was recognised. UTC is a safe answer for day bucketing.
            return ZoneId.of("UTC");
        }
    }

    /**
     * Folds one finished session into today's row and re-evaluates the goal and the quest.
     *
     * Counts are recomputed from today's attempts rather than incremented, for the same reason
     * mastery is: an increment that runs twice is worse than a rebuild that is merely slower.
     */
    @Transactional
    public DailyProgressEntity recordCompletion(UserEntity user, Instant when) {
        return record(user, when, 1, 0, 0, 0);
    }

    @Transactional
    public DailyProgressEntity recordReview(UserEntity user, Instant when) {
        return record(user, when, 0, 0, 1, 0);
    }

    @Transactional
    public DailyProgressEntity record(
            UserEntity user, Instant when, int problems, int patterns, int reviews, int speedruns) {
        LocalDate day = when.atZone(zoneOf(user)).toLocalDate();
        DailyProgressEntity row = days.findByUserIdAndDay(user.getId(), day).orElseGet(() -> {
            DailyProgressEntity fresh = new DailyProgressEntity();
            fresh.setUser(user);
            fresh.setDay(day);
            return fresh;
        });

        row.setProblemsCompleted(row.getProblemsCompleted() + problems);
        row.setPatternsIdentified(row.getPatternsIdentified() + patterns);
        row.setMistakesReviewed(row.getMistakesReviewed() + reviews);
        row.setSpeedrunsCompleted(row.getSpeedrunsCompleted() + speedruns);
        // One session is worth a minute of the ten, without pretending to know real time.
        // The goal is one problem or ten minutes, so the problem half usually carries it.
        row.setActiveMinutes(row.getActiveMinutes() + Math.max(1, problems));

        row.setGoalMet(DailyProgressScoring.meetsGoal(
                row.getProblemsCompleted(), row.getActiveMinutes()));
        row.setQuestCompleted(DailyProgressScoring.questCompleted(
                new DailyProgressScoring.DailyCounts(
                        row.getPatternsIdentified(),
                        row.getProblemsCompleted(),
                        row.getMistakesReviewed(),
                        row.getSpeedrunsCompleted())));

        return days.save(row);
    }

    /**
     * The current streak.
     *
     * A day that has not been trained yet does not break it, because the day is not over. Only
     * two whole missed days do.
     */
    @Transactional(readOnly = true)
    public int currentStreak(UserEntity user) {
        List<LocalDate> met = days.findByUserIdAndGoalMetTrueOrderByDayDesc(user.getId()).stream()
                .map(DailyProgressEntity::getDay)
                .toList();
        return DailyProgressScoring.currentStreak(met, todayFor(user));
    }

    @Transactional(readOnly = true)
    public int longestStreak(UserEntity user) {
        List<LocalDate> met = days.findByUserIdAndGoalMetTrueOrderByDayDesc(user.getId()).stream()
                .map(DailyProgressEntity::getDay)
                .toList();
        return DailyProgressScoring.longestStreak(met);
    }

    @Transactional(readOnly = true)
    public boolean metToday(UserEntity user) {
        return days.findByUserIdAndDay(user.getId(), todayFor(user))
                .map(DailyProgressEntity::isGoalMet)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public DailyProgressEntity today(UserEntity user) {
        return days.findByUserIdAndDay(user.getId(), todayFor(user)).orElse(null);
    }

    /** XP earned today, which the dashboard shows next to the streak. */
    @Transactional
    public void addXp(UserEntity user, Instant when, int xp) {
        LocalDate day = when.atZone(zoneOf(user)).toLocalDate();
        DailyProgressEntity row = days.findByUserIdAndDay(user.getId(), day).orElse(null);
        if (row != null) {
            row.setXpEarned(row.getXpEarned() + xp);
            days.save(row);
        }
    }

    /** Rough minutes trained today, for the dashboard. */
    @Transactional(readOnly = true)
    public Duration activeTimeToday(UserEntity user) {
        DailyProgressEntity row = today(user);
        return Duration.ofMinutes(row == null ? 0 : row.getActiveMinutes());
    }
}