package com.patternrun.progress;

import static com.patternrun.progress.AttemptFixtures.abandoned;
import static com.patternrun.progress.AttemptFixtures.attempt;
import static com.patternrun.progress.AttemptFixtures.clean;
import static com.patternrun.progress.AttemptFixtures.imported;
import static com.patternrun.progress.AttemptFixtures.overallOf;
import static com.patternrun.progress.AttemptFixtures.poor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.patternrun.problem.Difficulty;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MasteryScoringTest {

    @Nested
    @DisplayName("recognition")
    class Recognition {

        @Test
        @DisplayName("is absent rather than zero when nothing has been attempted")
        void absentWithNoAttempts() {
            assertThat(MasteryScoring.recognition(List.of())).isNull();
        }

        @Test
        @DisplayName("is the share of attempts that named the right pattern")
        void ratesCorrectly() {
            List<com.patternrun.attempt.AttemptEntity> attempts =
                    List.of(clean(Difficulty.EASY, 60_000L), poor(Difficulty.EASY), clean(Difficulty.EASY, 90_000L));

            assertThat(MasteryScoring.recognition(attempts)).isEqualByComparingTo("66.67");
        }

        @Test
        @DisplayName("cannot be raised by solving more problems badly")
        void resistsGrinding() {
            List<com.patternrun.attempt.AttemptEntity> attempts = java.util.stream.IntStream.range(0, 25)
                    .mapToObj(i -> poor(Difficulty.EASY))
                    .toList();

            assertThat(MasteryScoring.recognition(attempts)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("counts history recovered from the browser")
        void includesImportedHistory() {
            assertThat(MasteryScoring.recognition(List.of(imported(true, true, 1), imported(false, true, 3))))
                    .isEqualByComparingTo("50.00");
        }
    }

    @Nested
    @DisplayName("correctness")
    class Correctness {

        @Test
        @DisplayName("is absent when nothing has been attempted")
        void absentWithNoAttempts() {
            assertThat(MasteryScoring.correctness(List.of())).isNull();
        }

        @Test
        @DisplayName("uses complexity alone when there were no predictions")
        void complexityOnly() {
            assertThat(MasteryScoring.correctness(List.of(clean(Difficulty.EASY, 60_000L))))
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("drops when predictions were wrong even though complexity was right")
        void predictionsPullItDown() {
            com.patternrun.attempt.AttemptEntity attempt = clean(Difficulty.EASY, 60_000L);
            attempt.setPredictions(List.of(
                    new com.patternrun.attempt.PredictionRecord(1, 0, false),
                    new com.patternrun.attempt.PredictionRecord(2, 0, true)));

            // 0.4 complexity + 0.6 * 0.5 predictions
            assertThat(MasteryScoring.correctness(List.of(attempt)))
                    .isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("weights a hard problem above an easy one")
        void harderProblemScoresHigher() {
            // Only visible when difficulties are mixed. A single perfect attempt clamps to 100
            // whatever its difficulty, so comparing two of them would prove nothing.
            BigDecimal withHard = MasteryScoring.correctness(
                    List.of(poor(Difficulty.EASY), clean(Difficulty.HARD, 60_000L)));
            BigDecimal withEasy = MasteryScoring.correctness(
                    List.of(poor(Difficulty.EASY), clean(Difficulty.EASY, 60_000L)));

            assertThat(withHard).isGreaterThan(withEasy);
        }

        @Test
        @DisplayName("never exceeds 100 however hard the problem was")
        void clampsAtHundred() {
            // Weight 1.5 applied to a perfect 100 would be 150 without the clamp.
            assertThat(MasteryScoring.correctness(List.of(clean(Difficulty.HARD, 60_000L))))
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("excludes attempts that were never completed")
        void excludesAbandoned() {
            assertThat(MasteryScoring.correctness(List.of(abandoned()))).isNull();
            assertThat(MasteryScoring.recognition(List.of(abandoned()))).isNull();
        }
    }

    @Nested
    @DisplayName("speed")
    class Speed {

        private BigDecimal scale(String value) {
            return new BigDecimal(value);
        }

        @Test
        @DisplayName("is absent, not zero, when no attempt carries a duration")
        void absentWithoutTiming() {
            // The distinction that matters: an untimed learner is not a slow one.
            assertThat(MasteryScoring.speed(List.of(imported(true, true, 0)))).isNull();
        }

        @Test
        @DisplayName("is absent when there are no attempts at all")
        void absentWithNoAttempts() {
            assertThat(MasteryScoring.speed(List.of())).isNull();
        }

        @Test
        @DisplayName("is full marks when beating the expected time")
        void fasterThanExpected() {
            assertThat(MasteryScoring.speed(List.of(clean(Difficulty.EASY, 60_000L))))
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("is half when taking twice the expected time")
        void twiceAsSlow() {
            // EASY expects 300s; 600s is double.
            assertThat(MasteryScoring.speed(List.of(clean(Difficulty.EASY, 600_000L))))
                    .isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("averages only the attempts that were actually timed")
        void ignoresUntimedAttempts() {
            // Only the two timed attempts count; the imported one has no duration.
            assertThat(MasteryScoring.speed(List.of(
                            clean(Difficulty.EASY, 60_000L),
                            imported(true, true, 0),
                            clean(Difficulty.EASY, 120_000L))))
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("keeps discriminating between genuinely different speeds")
        void discriminates() {
            // EASY expects 300s, so 300s scores 100 and 600s scores 50. Averaging first keeps
            // the spread visible; clamping each attempt first would have flattened both to 100
            // and 50 for any pair containing one fast run.
            assertThat(MasteryScoring.speed(List.of(
                            clean(Difficulty.EASY, 300_000L),
                            clean(Difficulty.EASY, 600_000L))))
                    .isEqualByComparingTo("75.00");

        }

        @Test
        @DisplayName("falls as the time goes up")
        void monotonic() {
            // The axis is a mean, so one blazing run can pull it up. That is deliberate: a single
            // outlier should not dominate a pattern's speed. What must hold is that taking
            // longer never scores higher.
            BigDecimal atSixMinutes = MasteryScoring.speed(List.of(clean(Difficulty.EASY, 360_000L)));
            BigDecimal atTwelveMinutes = MasteryScoring.speed(List.of(clean(Difficulty.EASY, 720_000L)));

            assertThat(atTwelveMinutes).isLessThan(atSixMinutes);
            assertThat(atSixMinutes).isGreaterThan(scale("0"));
        }
    }

    @Nested
    @DisplayName("overall (README section 37)")
    class Overall {

        @Test
        @DisplayName("is absent when no axis has been measured")
        void absentWithNothingMeasured() {
            assertThat(overallOf(null, null, null, null, null)).isNull();
        }

        @Test
        @DisplayName("normalises over the axes that exist rather than capping at 70")
        void renormalisesOverMeasuredAxes() {
            // The literal formula sums to 1.00 over five axes. With only two available it would
            // give 0.55 * 100 = 55 for perfect work, which reads as failure.
            assertThat(overallOf(scale("100"), scale("100"), null, null, null))
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("respects the relative weights of the measured axes")
        void respectsWeights() {
            // 0.25 * 100 over (0.25 + 0.30): the one full-mark axis carries the whole score.
            assertThat(overallOf(scale("100"), scale("0"), null, null, null))
                    .isEqualByComparingTo("45.45");
        }

        @Test
        @DisplayName("folds a later axis in without changing the others")
        void foldsInLaterAxes() {
            // Retention starting to be measured, at 40, has to drag the average down. An axis
            // that only ever arrives at full marks would leave an all-100 score untouched, so
            // this would pass without proving the axis is being included at all.
            assertThat(overallOf(scale("100"), scale("100"), scale("100"), null, scale("40")))
                    .isLessThan(overallOf(scale("100"), scale("100"), scale("100"), null, null));
        }

        @Test
        @DisplayName("stays within 0 to 100 whatever is passed in")
        void alwaysBounded() {
            assertThat(overallOf(scale("0"), scale("0"), scale("0"), scale("0"), scale("0")))
                    .isEqualByComparingTo("0.00");
            assertThat(overallOf(scale("100"), scale("100"), scale("100"), scale("100"), scale("100")))
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("is not raised by volume alone")
        void resistsGrinding() {
            List<com.patternrun.attempt.AttemptEntity> grinding = java.util.stream.IntStream.range(0, 30)
                    .mapToObj(i -> poor(Difficulty.EASY))
                    .toList();

            assertThat(overallFromAttempts(grinding))
                    .isLessThanOrEqualTo(scale("40"));
        }

        private BigDecimal overallFromAttempts(List<com.patternrun.attempt.AttemptEntity> attempts) {
            return MasteryScoring.overall(MasteryScoring.axesOf(
                    MasteryScoring.recognition(attempts),
                    MasteryScoring.correctness(attempts),
                    MasteryScoring.speed(attempts),
                    null,
                    null));
        }

        private BigDecimal scale(String value) {
            return new BigDecimal(value);
        }
    }

    @Nested
    @DisplayName("grade (README section 97)")
    class Grade {

        @Test
        @DisplayName("labels a perfect attempt EXCELLENT")
        void perfect() {
            assertThat(MasteryScoring.grade(clean(Difficulty.EASY, 60_000L))).isEqualTo("EXCELLENT");
        }

        @Test
        @DisplayName("labels a wrong pattern and wrong complexity NEEDS_REVIEW")
        void wrongEverything() {
            assertThat(MasteryScoring.grade(poor(Difficulty.EASY))).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("uses the section 97 boundaries")
        void boundaries() {
            assertThat(MasteryScoring.gradeFor(90)).isEqualTo("EXCELLENT");
            assertThat(MasteryScoring.gradeFor(89.9)).isEqualTo("STRONG");
            assertThat(MasteryScoring.gradeFor(75)).isEqualTo("STRONG");
            assertThat(MasteryScoring.gradeFor(74.9)).isEqualTo("DEVELOPING");
            assertThat(MasteryScoring.gradeFor(60)).isEqualTo("DEVELOPING");
            assertThat(MasteryScoring.gradeFor(59.9)).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("renormalises over the rubric an attempt can evidence")
        void renormalisesRubric() {
            // Pattern, complexity and breakdown all correct and no timing: 40 of 50, which is
            // full marks for what was actually asked.
            com.patternrun.attempt.AttemptEntity untimed = clean(Difficulty.EASY, 60_000L);
            untimed.setDurationMs(null);

            assertThat(MasteryScoring.scoreOutOf100(untimed)).isCloseTo(100, within(0.01));
        }

        @Test
        @DisplayName("marks an untimed grade provisional")
        void provisionalWhenUntimed() {
            com.patternrun.attempt.AttemptEntity untimed = clean(Difficulty.EASY, 60_000L);
            untimed.setDurationMs(null);

            assertThat(MasteryScoring.isProvisionalGrade(untimed)).isTrue();
            assertThat(MasteryScoring.isProvisionalGrade(clean(Difficulty.EASY, 60_000L))).isFalse();
        }
    }

    @Test
    @DisplayName("only completed attempts count as evidence")
    void completedOnly() {
        var attempts = List.of(abandoned(), attempt());

        assertThat(MasteryScoring.completedOnly(attempts)).hasSize(1);
    }
}