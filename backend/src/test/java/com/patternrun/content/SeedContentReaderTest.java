package com.patternrun.content;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import com.patternrun.content.seed.ProblemSeed;
import com.patternrun.content.seed.SeedContent;
import com.patternrun.problem.AnimationStepType;
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
            assertThat(problem.hints()).extracting(ProblemSeed.HintSeed::level)
                    .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
            assertThat(problem.animationSteps()).anyMatch(step -> step.type() == AnimationStepType.QUESTION);
            assertThat(problem.animationSteps()).allMatch(step -> !step.text().isBlank());
            assertThat(problem.testCases()).anyMatch(ProblemSeed.TestCaseSeed::hidden);
            assertThat(problem.solutions()).anyMatch(solution -> solution.language().name().equals("JAVA"));
        });
    }
}