package com.patternrun.content;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import com.patternrun.content.seed.ProblemSeed;
import com.patternrun.content.seed.SeedContent;
import com.patternrun.problem.AnimationStepType;
import com.patternrun.problem.HintStage;
import com.patternrun.problem.ProgrammingLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class SeedContentReaderTest {

    private SeedContentReader reader;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        reader = new SeedContentReader(new ObjectMapper(), validator);
    }

    @Test
    @DisplayName("The shipped content is readable from the single seed location")
    void findsShippedContent() throws IOException {
        assertThat(new PathMatchingResourcePatternResolver().getResources("classpath*:seed/problems/*.json"))
                .hasSizeGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("Reads ten patterns and twenty problems")
    void readsShippedContent() {
        SeedContent content = reader.read();

        assertThat(content.patterns()).hasSize(10);
        assertThat(content.problems()).hasSize(20);
        assertThat(content.patternSlugs()).contains("hashing", "sliding-window", "binary-search", "dp");
    }

    @Test
    @DisplayName("Patterns are ordered by the learning path and problems by problem number")
    void ordersContent() {
        SeedContent content = reader.read();

        assertThat(content.patterns()).extracting("difficultyOrder")
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(content.problems()).extracting("externalId").isSorted();
    }

    @Test
    @DisplayName("Every problem carries a complete hint ladder, hidden tests and a predict-the-move question")
    void everyProblemSupportsTheTrainingLoop() {
        SeedContent content = reader.read();

        assertThat(content.problems()).allSatisfy(problem -> {
            // The reasoning ladder is what has to be complete, and it stays exactly five rungs.
            // Phase 4 added per-stage rungs on top, and asserting on the raw hint list would
            // instead be asserting that no problem has ever been given a debugging hint.
            assertThat(problem.hints().stream()
                    .filter(ProblemSeed.HintSeed::isReasoningRung)
                    .map(ProblemSeed.HintSeed::level))
                    .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
            assertThat(problem.animationSteps()).anyMatch(step -> step.type() == AnimationStepType.QUESTION);
            assertThat(problem.animationSteps()).allMatch(step -> !step.text().isBlank());
            assertThat(problem.testCases()).anyMatch(ProblemSeed.TestCaseSeed::hidden);
            assertThat(problem.solutions()).anyMatch(solution -> solution.language().name().equals("JAVA"));
        });
    }

    /**
     * A staged hint names the stage it is for.
     *
     * An unstaged hint that carries a trigger could never be selected, because the selector always
     * asks for a concrete stage. It would sit in the content looking like a debugging hint and
     * never appear, which is worse than not having written it.
     */
    @Test
    @DisplayName("A hint that is not a reasoning rung names the stage it is for")
    void stagedHintsDeclareAStage() {
        SeedContent content = reader.read();

        content.problems().forEach(problem -> assertThat(problem.hints())
                .filteredOn(hint -> !hint.isReasoningRung())
                .allSatisfy(hint -> assertThat(hint.stageOrAny())
                        .as("%s level %d is staged, so it needs a real stage", problem.slug(), hint.level())
                        .isNotEqualTo(HintStage.ANY)));

        // And at least one problem actually has one, so this cannot pass by asserting nothing.
        assertThat(content.problems().stream()
                .flatMap(problem -> problem.hints().stream())
                .filter(hint -> hint.stageOrAny() != HintStage.ANY))
                .as("the shipped content should exercise staged hints")
                .isNotEmpty();
    }

    /**
     * The pairing that makes a runnable problem runnable, checked on both sides.
     *
     * A half-configured problem is the dangerous shape: an entrypoint with no structured arguments
     * offers an editor and then fails every submission as a wrong answer, which teaches the learner
     * something false about their own code.
     */
    @Test
    @DisplayName("Runnable content is configured all the way through, or not at all")
    void runnableContentIsComplete() {
        SeedContent content = reader.read();

        assertThat(content.problems()).allSatisfy(problem -> {
            long runnableCases = problem.testCases().stream()
                    .filter(ProblemSeed.TestCaseSeed::isRunnable)
                    .count();

            if (problem.entrypoint() == null) {
                assertThat(runnableCases)
                        .as("%s has no entrypoint, so no case should claim to be runnable", problem.slug())
                        .isZero();
            } else {
                assertThat(runnableCases)
                        .as("%s declares an entrypoint, so it needs runnable cases", problem.slug())
                        .isPositive();
                assertThat(problem.solutions())
                        .as("%s is runnable and needs a Python reference to reveal", problem.slug())
                        .anyMatch(solution -> solution.language() == ProgrammingLanguage.PYTHON);
            }
        });

        // Two Sum is the vertical slice, so it is the one that has to be fully wired.
        ProblemSeed twoSum = content.problems().stream()
                .filter(problem -> problem.slug().equals("two-sum"))
                .findFirst()
                .orElseThrow();
        assertThat(twoSum.entrypoint()).isEqualTo("two_sum");
        assertThat(twoSum.testCases()).filteredOn(ProblemSeed.TestCaseSeed::isRunnable).isNotEmpty();
    }
}