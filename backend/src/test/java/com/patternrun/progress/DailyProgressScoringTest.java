package com.patternrun.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DailyProgressScoringTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 14);

    @Nested
    @DisplayName("daily goal (README section 34)")
    class Goal {

        @Test
        @DisplayName("is one problem")
        void oneProblem() {
            assertThat(DailyProgressScoring.meetsGoal(1, 0)).isTrue();
        }

        @Test
        @DisplayName("is ten minutes")
        void tenMinutes() {
            assertThat(DailyProgressScoring.meetsGoal(0, 10)).isTrue();
        }

        @Test
        @DisplayName("is not a quota of problems")
        void notAQuota() {
            // One small thing counts. The spec asks for a sustainable goal.
            assertThat(DailyProgressScoring.meetsGoal(0, 2)).isFalse();
            assertThat(DailyProgressScoring.meetsGoal(0, 9)).isFalse();
        }
    }

    @Nested
    @DisplayName("streak")
    class Streak {

        @Test
        @DisplayName("is zero with no history")
        void empty() {
            assertThat(DailyProgressScoring.currentStreak(List.of(), TODAY)).isZero();
            assertThat(DailyProgressScoring.longestStreak(List.of())).isZero();
        }

        @Test
        @DisplayName("counts back from today")
        void countsFromToday() {
            assertThat(DailyProgressScoring.currentStreak(
                    List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(3);
        }

        @Test
        @DisplayName("survives a today that has not been trained yet")
        void todayStillOpen() {
            // The day is not over. Opening the app in the morning must not show a lost streak.
            assertThat(DailyProgressScoring.currentStreak(
                    List.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(2);
        }

        @Test
        @DisplayName("breaks once two whole days have been missed")
        void breaksAfterTwoDays() {
            assertThat(DailyProgressScoring.currentStreak(
                    List.of(TODAY.minusDays(3), TODAY.minusDays(4)), TODAY)).isZero();
        }

        @Test
        @DisplayName("stops at the first gap")
        void stopsAtGap() {
            assertThat(DailyProgressScoring.currentStreak(
                    List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(4), TODAY.minusDays(5)), TODAY))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("is not confused by unsorted or duplicated input")
        void toleratesMessyInput() {
            assertThat(DailyProgressScoring.currentStreak(
                    List.of(TODAY.minusDays(2), TODAY, TODAY, TODAY.minusDays(1)), TODAY)).isEqualTo(3);
        }

        @Test
        @DisplayName("remembers the best run even after it is broken")
        void longestSurvives() {
            List<LocalDate> history = List.of(
                    TODAY.minusDays(10), TODAY.minusDays(9), TODAY.minusDays(8),
                    TODAY.minusDays(7), TODAY.minusDays(6), TODAY.minusDays(5),
                    TODAY.minusDays(2), TODAY.minusDays(1));

            assertThat(DailyProgressScoring.currentStreak(history, TODAY)).isEqualTo(2);
            assertThat(DailyProgressScoring.longestStreak(history)).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("daily quest (README section 35)")
    class Quest {

        @Test
        @DisplayName("needs all four items")
        void needsAll() {
            assertThat(DailyProgressScoring.questCompleted(
                    new DailyProgressScoring.DailyCounts(3, 1, 1, 1))).isTrue();
            assertThat(DailyProgressScoring.questCompleted(
                    new DailyProgressScoring.DailyCounts(3, 1, 1, 0))).isFalse();
            assertThat(DailyProgressScoring.questCompleted(
                    new DailyProgressScoring.DailyCounts(2, 1, 1, 1))).isFalse();
            assertThat(DailyProgressScoring.questCompleted(
                    new DailyProgressScoring.DailyCounts(0, 0, 0, 0))).isFalse();
        }

        @Test
        @DisplayName("reports partial progress so the list can tick off")
        void partialProgress() {
            assertThat(DailyProgressScoring.questItemsCompleted(
                    new DailyProgressScoring.DailyCounts(3, 1, 0, 0))).isEqualTo(2);
            assertThat(DailyProgressScoring.questItemsCompleted(
                    new DailyProgressScoring.DailyCounts(0, 0, 0, 0))).isZero();
            assertThat(DailyProgressScoring.questItemsCompleted(
                    new DailyProgressScoring.DailyCounts(9, 9, 9, 9))).isEqualTo(4);
        }
    }
}