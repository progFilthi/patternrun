package com.patternrun.attempt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CorrectnessCheckingTest {

    @Nested
    @DisplayName("complexity comparison")
    class Complexity {

        @Test
        @DisplayName("folds the superscript and the caret to one form")
        void superscriptAndCaretAgree() {
            // The regression that mattered: the seed writes O(n^2) and the options offer O(n²),
            // so a quadratic problem had no reachable correct answer at all.
            assertThat(CorrectnessChecking.complexityMatches("O(n²)", "O(n^2)")).isTrue();
            assertThat(CorrectnessChecking.complexityMatches("O(n^2)", "O(n²)")).isTrue();
            assertThat(CorrectnessChecking.complexityMatches("O(n³)", "O(n^3)")).isTrue();
        }

        @Test
        @DisplayName("ignores case and surrounding space")
        void ignoresCaseAndSpace() {
            assertThat(CorrectnessChecking.complexityMatches("  o(n) ", "O(n)")).isTrue();
            assertThat(CorrectnessChecking.complexityMatches("O(LOG N)", "O(log n)")).isTrue();
        }

        @Test
        @DisplayName("keeps genuinely different answers apart")
        void differentAnswersDiffer() {
            assertThat(CorrectnessChecking.complexityMatches("O(n)", "O(n log n)")).isFalse();
            assertThat(CorrectnessChecking.complexityMatches("O(1)", "O(n)")).isFalse();
            assertThat(CorrectnessChecking.complexityMatches("O(n + m)", "O(n)")).isFalse();
        }

        @Test
        @DisplayName("never treats an absent answer as right")
        void absenceIsNotCorrect() {
            assertThat(CorrectnessChecking.complexityMatches(null, "O(n)")).isFalse();
            assertThat(CorrectnessChecking.complexityMatches("", "O(n)")).isFalse();
            assertThat(CorrectnessChecking.complexityMatches("O(n)", null)).isFalse();
        }

        @Test
        @DisplayName("needs both halves to be right")
        void bothHalvesRequired() {
            assertThat(CorrectnessChecking.complexityCorrect("O(n)", "O(n)", "O(n)", "O(n)")).isTrue();
            // Half right is not right: a learner who says O(n) time and O(1) space has not
            // reasoned about the algorithm.
            assertThat(CorrectnessChecking.complexityCorrect("O(n)", "O(1)", "O(n)", "O(n)")).isFalse();
            assertThat(CorrectnessChecking.complexityCorrect("O(n^2)", "O(n)", "O(n)", "O(n)")).isFalse();
        }

        @Test
        @DisplayName("inverts cleanly")
        void inverse() {
            assertThat(CorrectnessChecking.complexityIncorrect("O(n)", "O(n)", "O(n)", "O(n)")).isFalse();
            assertThat(CorrectnessChecking.complexityIncorrect("O(n²)", "O(n)", "O(n^2)", "O(n)")).isFalse();
            assertThat(CorrectnessChecking.complexityIncorrect("O(1)", "O(n)", "O(n)", "O(n)")).isTrue();
        }
    }

    @Nested
    @DisplayName("predictions")
    class Predictions {

        @Test
        @DisplayName("matches only the answer the step holds")
        void matchesAnswerIndex() {
            assertThat(CorrectnessChecking.predictionCorrect(1, 1)).isTrue();
            assertThat(CorrectnessChecking.predictionCorrect(0, 1)).isFalse();
        }

        @Test
        @DisplayName("treats a step with no recorded answer as unanswerable")
        void noAnswerIsNeverCorrect() {
            assertThat(CorrectnessChecking.predictionCorrect(0, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("duration")
    class Duration {

        @Test
        @DisplayName("accepts a plausible session")
        void plausible() {
            assertThat(CorrectnessChecking.isPlausibleDuration(180_000L)).isTrue();
            assertThat(CorrectnessChecking.isPlausibleDuration(null)).isTrue();
        }

        @Test
        @DisplayName("rejects a broken clock rather than storing it")
        void implausible() {
            assertThat(CorrectnessChecking.isPlausibleDuration(7L)).isFalse();
            assertThat(CorrectnessChecking.isPlausibleDuration(0L)).isFalse();
            assertThat(CorrectnessChecking.isPlausibleDuration(9_000_000L)).isFalse();
        }

        @Test
        @DisplayName("drops an implausible value to null, not to zero")
        void dropsToNull() {
            // Zero would permanently poison the speed axis for that problem.
            assertThat(CorrectnessChecking.sanitiseDuration(7L)).isNull();
            assertThat(CorrectnessChecking.sanitiseDuration(180_000L)).isEqualTo(180_000L);
            assertThat(CorrectnessChecking.sanitiseDuration(null)).isNull();
        }
    }

    @Nested
    @DisplayName("pattern guess")
    class PatternGuess {

        @Test
        @DisplayName("matches the problem's primary pattern")
        void matches() {
            assertThat(CorrectnessChecking.patternCorrect("hashing", "hashing")).isTrue();
            assertThat(CorrectnessChecking.patternCorrect("Hashing", "hashing")).isTrue();
            assertThat(CorrectnessChecking.patternCorrect(" sliding-window ", "sliding-window")).isTrue();
        }

        @Test
        @DisplayName("rejects anything else, including no answer")
        void rejects() {
            assertThat(CorrectnessChecking.patternCorrect("dp", "hashing")).isFalse();
            assertThat(CorrectnessChecking.patternCorrect(null, "hashing")).isFalse();
            assertThat(CorrectnessChecking.patternCorrect("hashing", null)).isFalse();
        }
    }
}