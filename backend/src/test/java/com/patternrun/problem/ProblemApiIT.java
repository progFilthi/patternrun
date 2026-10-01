package com.patternrun.problem;

import static org.hamcrest.Matchers.contains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patternrun.support.ApiIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ProblemApiIT extends ApiIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("GET /api/v1/problems returns a paginated list ordered by problem number")
    void returnsPaginatedProblems() throws Exception {
        mockMvc.perform(get("/api/v1/problems").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", is(20)))
                .andExpect(jsonPath("$.totalPages", is(4)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.content[0].slug", is("two-sum")))
                .andExpect(jsonPath("$.content[0].externalId", is(1)))
                .andExpect(jsonPath("$.content[0].pattern.slug", is("hashing")))
                .andExpect(jsonPath("$.content[0].complexity.time", is("O(n)")));
    }

    @Test
    @DisplayName("GET /api/v1/problems?pattern= filters by pattern slug")
    void filtersByPattern() throws Exception {
        mockMvc.perform(get("/api/v1/problems").param("pattern", "sliding-window"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].slug", contains(
                        "longest-substring-without-repeating-characters",
                        "minimum-window-substring")));
    }

    @Test
    @DisplayName("GET /api/v1/problems?pattern=unknown returns 404")
    void rejectsUnknownPatternFilter() throws Exception {
        mockMvc.perform(get("/api/v1/problems").param("pattern", "quantum-heating"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("quantum-heating")));
    }

    @Test
    @DisplayName("GET /api/v1/problems?difficulty= filters by difficulty")
    void filtersByDifficulty() throws Exception {
        mockMvc.perform(get("/api/v1/problems").param("difficulty", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].difficulty", org.hamcrest.Matchers.everyItem(is("MEDIUM"))));
    }

    @Test
    @DisplayName("GET /api/v1/problems/{slug} returns the full problem anatomy")
    void returnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/problems/two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is("two-sum")))
                .andExpect(jsonPath("$.title", is("Two Sum")))
                .andExpect(jsonPath("$.difficulty", is("EASY")))
                .andExpect(jsonPath("$.trainingDifficulty", is("RECOGNITION")))
                .andExpect(jsonPath("$.statement", containsString("target")))
                .andExpect(jsonPath("$.constraints", hasSize(4)))
                .andExpect(jsonPath("$.examples", hasSize(2)))
                .andExpect(jsonPath("$.pseudocode", hasSize(7)))
                .andExpect(jsonPath("$.complexity.time", is("O(n)")))
                .andExpect(jsonPath("$.complexity.space", is("O(n)")))
                .andExpect(jsonPath("$.whyThisPattern", containsString("HashMap")))
                .andExpect(jsonPath("$.bruteForce", containsString("O(n^2)")))
                .andExpect(jsonPath("$.invariant", containsString("seen")))
                .andExpect(jsonPath("$.interviewExplanation", containsString("HashMap")))
                .andExpect(jsonPath("$.commonMistakes", hasSize(3)))
                .andExpect(jsonPath("$.secondaryPatterns[0].slug", is("two-pointers")))
                .andExpect(jsonPath("$.hintCount", is(5)))
                .andExpect(jsonPath("$.animationStepCount", is(8)))
                .andExpect(jsonPath("$.visibleTestCaseCount", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/problems/{slug}/hints returns the five level ladder")
    void returnsHintLadder() throws Exception {
        mockMvc.perform(get("/api/v1/problems/two-sum/hints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].level", contains(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$[0].content", containsString("information")));
    }

    @Test
    @DisplayName("GET /api/v1/problems/{slug}/animation returns steps with a text alternative each")
    void returnsAnimationSteps() throws Exception {
        mockMvc.perform(get("/api/v1/problems/two-sum/animation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order", is(1)))
                .andExpect(jsonPath("$[0].type", is("TEXT")))
                .andExpect(jsonPath("$[0].text", containsString("two numbers")))
                .andExpect(jsonPath("$[1].type", is("ARRAY")))
                .andExpect(jsonPath("$[1].payload.values[0]", is(2)))
                .andExpect(jsonPath("$[2].type", is("QUESTION")))
                .andExpect(jsonPath("$[2].payload.options", hasSize(4)))
                .andExpect(jsonPath("$[2].payload.answerIndex", is(1)))
                .andExpect(jsonPath("$[7].type", is("SUCCESS")));
    }

    @Test
    @DisplayName("GET /api/v1/problems/{slug}/test-cases never exposes hidden cases")
    void returnsVisibleTestCasesOnly() throws Exception {
        mockMvc.perform(get("/api/v1/problems/two-sum/test-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].label", contains("Example 1", "Negative values")))
                .andExpect(jsonPath("$[*].isHidden").doesNotExist());
    }

    @Test
    @DisplayName("No endpoint leaks a hidden test case")
    void neverExposesHiddenTestCases() throws Exception {
        Integer hiddenCases = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM problem_test_cases WHERE is_hidden", Integer.class);
        assertThat(hiddenCases).isPositive();

        String[] problemSlugs = {"two-sum", "climbing-stairs", "minimum-window-substring", "number-of-islands"};
        for (String slug : problemSlugs) {
            Integer total = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM problem_test_cases t JOIN problems p ON p.id = t.problem_id"
                            + " WHERE p.slug = ?", Integer.class, slug);
            Integer visible = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM problem_test_cases t JOIN problems p ON p.id = t.problem_id"
                            + " WHERE p.slug = ? AND t.is_hidden = false", Integer.class, slug);

            mockMvc.perform(get("/api/v1/problems/{slug}", slug))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("hidden"))));
            mockMvc.perform(get("/api/v1/problems/{slug}/test-cases", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(visible)));

            assertThat(total).as("total cases for %s", slug).isGreaterThan(visible);
        }
    }

    @Test
    @DisplayName("Bean Validation rejects out of range pagination and unknown sort fields")
    void validatesQueryParameters() throws Exception {
        mockMvc.perform(get("/api/v1/problems").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
        mockMvc.perform(get("/api/v1/problems").param("size", "1000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/problems").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/problems").param("sort", "nonsense"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nonsense")));
        mockMvc.perform(get("/api/v1/problems").param("pattern", "Not A Slug"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unknown slugs return a clean 404 without stack traces")
    void unknownProblemReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/problems/no-such-problem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.path", is("/api/v1/problems/no-such-problem")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}