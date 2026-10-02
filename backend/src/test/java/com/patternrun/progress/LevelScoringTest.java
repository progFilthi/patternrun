package com.patternrun.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LevelScoringTest {

    @Nested
    @DisplayName("levels (README section 102)")
    class Levels {

        @Test
        @DisplayName("starts at level one")
        void startsAtOne() {
            assertThat(LevelScoring.levelNumber(0)).isEqualTo(1);
            assertThat(LevelScoring.levelName(0)).isEqualTo("Rookie");
        }

        @Test
        @DisplayName("steps up at each threshold")
        void thresholds() {
            assertThat(LevelScoring.levelNumber(149)).isEqualTo(1);
            assertThat(LevelScoring.levelNumber(150)).isEqualTo(2);
            assertThat(LevelScoring.levelNumber(399)).isEqualTo(2);
            assertThat(LevelScoring.levelNumber(400)).isEqualTo(3);
            assertThat(LevelScoring.levelNumber(800)).isEqualTo(4);
            assertThat(LevelScoring.levelNumber(1400)).isEqualTo(5);
            assertThat(LevelScoring.levelNumber(2200)).isEqualTo(6);
            assertThat(LevelScoring.levelNumber(3200)).isEqualTo(7);
        }

        @Test
        @DisplayName("caps at the last level for very large totals")
        void capsAtTop() {
            assertThat(LevelScoring.levelNumber(1_000_000)).isEqualTo(7);
            assertThat(LevelScoring.levelProgress(1_000_000)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("reports progress through the current level")
        void progressWithinLevel() {
            assertThat(LevelScoring.levelProgress(0)).isEqualTo(0.0);
            assertThat(LevelScoring.levelProgress(75)).isCloseTo(0.5, within(0.001));
            assertThat(LevelScoring.levelProgress(150)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("never reports progress outside 0 to 1")
        void alwaysBounded() {
            for (long xp : new long[] {0, 1, 149, 150, 151, 400, 3200, 999999}) {
                assertThat(LevelScoring.levelProgress(xp)).isBetween(0.0, 1.0);
            }
        }
    }

    @Nested
    @DisplayName("combo (README section 33)")
    class Combo {

        @Test
        @DisplayName("starts at one and grows with a run")
        void grows() {
            assertThat(LevelScoring.comboMultiplier(0)).isEqualTo(1.0);
            assertThat(LevelScoring.comboMultiplier(1)).isEqualTo(1.0);
            assertThat(LevelScoring.comboMultiplier(2)).isEqualTo(2.0);
            assertThat(LevelScoring.comboMultiplier(3)).isEqualTo(3.0);
        }

        @Test
        @DisplayName("caps so it cannot outgrow the learning it rewards")
        void capped() {
            assertThat(LevelScoring.comboMultiplier(4)).isEqualTo(4.0);
            assertThat(LevelScoring.comboMultiplier(50)).isEqualTo(4.0);
        }

        @Test
        @DisplayName("resets on a break and grows otherwise")
        void breaksAndGrows() {
            assertThat(LevelScoring.nextComboAfter(false, 2)).isEqualTo(3);
            assertThat(LevelScoring.nextComboAfter(true, 7)).isZero();
            assertThat(LevelScoring.nextComboAfter(false, 0)).isEqualTo(1);
        }
    }
}