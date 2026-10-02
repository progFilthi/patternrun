package com.patternrun.mistake;

import static org.assertj.core.api.Assertions.assertThat;

import com.patternrun.attempt.AttemptEntity;
import com.patternrun.execution.ExecutionOutcome;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What counts as a mistake, and what does not.
 *
 * Pure tests on purpose. The classification is a judgement about a set of signals, and testing it
 * without a database means every branch is reachable in one read rather than through a fixture
 * per combination. It is also the class that was missing entirely: the journal had everything
 * except the code that would have put anything in it.
 */
class MistakeClassifierTest {

    private static AttemptEntity attempt() {
        AttemptEntity attempt = new AttemptEntity();
        // Defaults are the interesting case: a learner who did nothing wrong.
        attempt.setPatternCorrect(true);
        attempt.setComplexityCorrect(true);
        attempt.setBreakdownCorrect(true);
        return attempt;
    }

    @Nested
    @DisplayName("a clean attempt produces nothing")
    class Clean {

        @Test
        @DisplayName("everything correct is not a mistake")
        void allCorrect() {
            assertThat(MistakeClassifier.inspect(attempt())).isEmpty();
        }

        @Test
        @DisplayName("code that was never run is not a mistake either")
        void noCodeAttempt() {
            assertThat(MistakeClassifier.inspect(attempt())).isEmpty();
        }
    }

    @Nested
    @DisplayName("the code outcome classifies by what actually went wrong")
    class CodeOutcomes {

        @Test
        @DisplayName("a wrong answer is a logic problem")
        void wrongAnswer() {
            AttemptEntity attempt = attempt();
            attempt.setCodeOutcome(ExecutionOutcome.WRONG_ANSWER);

            assertThat(MistakeClassifier.primary(attempt))
                    .hasValueSatisfying(f -> assertThat(f.category()).isEqualTo(MistakeCategory.LOGIC));
        }

        /**
         * The distinction the whole mistake journal is for.
         *
         * A crash is a typo or a type, not a wrong idea. Filing it as LOGIC would send the learner
         * back to re-derive an algorithm that was already correct and away from the line that threw.
         */
        @Test
        @DisplayName("a crash is filed as a test failure, not a logic one")
        void runtimeErrorIsNotLogic() {
            AttemptEntity attempt = attempt();
            attempt.setCodeOutcome(ExecutionOutcome.RUNTIME_ERROR);

            assertThat(MistakeClassifier.primary(attempt))
                    .hasValueSatisfying(f -> assertThat(f.category()).isEqualTo(MistakeCategory.TESTS));
        }

        @Test
        @DisplayName("a syntax error is also a test failure")
        void syntaxError() {
            AttemptEntity attempt = attempt();
            attempt.setCodeOutcome(ExecutionOutcome.SYNTAX_ERROR);

            assertThat(MistakeClassifier.primary(attempt))
                    .hasValueSatisfying(f -> assertThat(f.category()).isEqualTo(MistakeCategory.TESTS));
        }

        @Test
        @DisplayName("a timeout is a complexity problem, never an index-arithmetic one")
        void timeoutIsComplexity() {
            AttemptEntity attempt = attempt();
            attempt.setCodeOutcome(ExecutionOutcome.TIME_LIMIT_EXCEEDED);

            assertThat(MistakeClassifier.primary(attempt))
                    .hasValueSatisfying(f -> assertThat(f.category()).isEqualTo(MistakeCategory.COMPLEXITY));
        }

        @Test
        @DisplayName("our own runner failing is not the learner's mistake")
        void internalErrorIsNotRecorded() {
            AttemptEntity attempt = attempt();
            attempt.setCodeOutcome(ExecutionOutcome.INTERNAL_ERROR);

            // Nothing was evaluated, so nothing was got wrong. Recording this would punish a
            // learner for our infrastructure.
            assertThat(MistakeClassifier.inspect(attempt)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the reasoning signals")
    class Reasoning {

        @Test
        @DisplayName("a misidentified pattern is a pattern mistake")
        void wrongPattern() {
            AttemptEntity attempt = attempt();
            attempt.setPatternCorrect(false);
            attempt.setPatternGuess("sliding-window");

            assertThat(MistakeClassifier.primary(attempt))
                    .hasValueSatisfying(f -> {
                        assertThat(f.category()).isEqualTo(MistakeCategory.PATTERN);
                        assertThat(f.description()).contains("sliding-window");
                    });
        }

        /**
         * The cap that keeps a queue workable.
         *
         * A learner who missed the pattern, the code and the complexity has one thing wrong. Three
         * entries would triple their queue for a single misunderstanding and train them to ignore it.
         */
        @Test
        @DisplayName("a wrong pattern outranks the softer signals it causes")
        void wrongPatternOutranksItsConsequences() {
            AttemptEntity attempt = attempt();
            attempt.setPatternCorrect(false);
            attempt.setPatternGuess("dp");
            attempt.setComplexityCorrect(false);
            attempt.setBreakdownCorrect(false);

            List<MistakeClassifier.Finding> findings = MistakeClassifier.inspect(attempt);

            assertThat(findings).hasSize(MistakeClassifier.MAX_PER_ATTEMPT);
            assertThat(findings.get(0).category()).isEqualTo(MistakeCategory.PATTERN);
        }

        @Test
        @DisplayName("a wrong complexity is only recorded once the pattern was right")
        void wrongComplexityNeedsTheRightPatternFirst() {
            AttemptEntity attempt = attempt();
            attempt.setPatternCorrect(false);
            attempt.setComplexityCorrect(false);

            // With the pattern wrong, the complexity is a consequence of it and says nothing
            // about this learner in particular.
            assertThat(MistakeClassifier.inspect(attempt))
                    .allSatisfy(f -> assertThat(f.category()).isEqualTo(MistakeCategory.PATTERN));
        }

        @Test
        @DisplayName("a wrong complexity with the right pattern is a complexity mistake")
        void wrongComplexityAlone() {
            AttemptEntity attempt = attempt();
            attempt.setComplexityCorrect(false);

            assertThat(MistakeClassifier.primary(attempt))
                    .hasValueSatisfying(f -> assertThat(f.category()).isEqualTo(MistakeCategory.COMPLEXITY));
        }

        @Test
        @DisplayName("a misread problem is an explanation mistake")
        void wrongBreakdown() {
            AttemptEntity attempt = attempt();
            attempt.setBreakdownCorrect(false);

            assertThat(MistakeClassifier.primary(attempt))
                    .hasValueSatisfying(f -> assertThat(f.category()).isEqualTo(MistakeCategory.EXPLANATION));
        }
    }

    @Test
    @DisplayName("a code failure outranks a wrong pattern, because it is the more specific fact")
    void codeOutranksPattern() {
        AttemptEntity attempt = attempt();
        attempt.setPatternCorrect(false);
        attempt.setPatternGuess("dp");
        attempt.setCodeOutcome(ExecutionOutcome.WRONG_ANSWER);

        assertThat(MistakeClassifier.primary(attempt))
                .hasValueSatisfying(f -> assertThat(f.category()).isEqualTo(MistakeCategory.LOGIC));
    }

    @Test
    @DisplayName("every finding carries a lesson, because a repetition is not a correction")
    void everyFindingCarriesALesson() {
        AttemptEntity attempt = attempt();
        attempt.setPatternCorrect(false);
        attempt.setCodeOutcome(ExecutionOutcome.RUNTIME_ERROR);

        assertThat(MistakeClassifier.inspect(attempt))
                .isNotEmpty()
                .allSatisfy(f -> {
                    assertThat(f.lesson()).isNotBlank();
                    assertThat(f.description()).isNotBlank();
                });
    }
}