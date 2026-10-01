package com.patternrun.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.patternrun.content.ContentSeeder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Guards the content quality bar from README section 92: a problem is only complete when
 * every part of the training loop exists. Content regressions must fail the build.
 */
class SeedContentIT extends ApiIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContentSeeder contentSeeder;

    @Test
    @DisplayName("The seed pass is idempotent: running it again changes nothing")
    void seedPassIsIdempotent() {
        List<Map<String, Object>> before = contentRowCounts();
        int problemsBefore = countProblems();

        contentSeeder.seed();

        assertThat(contentRowCounts()).isEqualTo(before);
        assertThat(countProblems()).isEqualTo(problemsBefore);
    }

    private int countProblems() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM problems", Integer.class);
        return count == null ? 0 : count;
    }

    private List<Map<String, Object>> contentRowCounts() {
        return jdbcTemplate.queryForList("""
                SELECT (SELECT count(*) FROM patterns)                                        AS patterns,
                       (SELECT count(*) FROM problems)                                        AS problems,
                       (SELECT count(*) FROM problem_examples)                                AS examples,
                       (SELECT count(*) FROM problem_hints)                                   AS hints,
                       (SELECT count(*) FROM problem_animation_steps)                         AS steps,
                       (SELECT count(*) FROM problem_test_cases)                              AS test_cases,
                       (SELECT count(*) FROM problem_test_cases WHERE is_hidden)              AS hidden_cases,
                       (SELECT count(*) FROM problem_solutions)                               AS solutions,
                       (SELECT count(*) FROM problem_secondary_patterns)                      AS secondary_links
                """);
    }

    @Test
    @DisplayName("The ten core patterns are seeded with mental model, signals and a template")
    void seedsTenPatterns() {
        List<Map<String, Object>> patterns = jdbcTemplate.queryForList(
                "SELECT slug, name, summary, signal, mental_model, recognition_rules::text AS recognition_rules,"
                        + " template::text AS template, invariant"
                        + " FROM patterns ORDER BY difficulty_order");

        assertThat(patterns).hasSize(10);
        assertThat(patterns).allSatisfy(pattern -> {
            assertThat((String) pattern.get("name")).isNotBlank();
            assertThat((String) pattern.get("summary")).isNotBlank();
            assertThat((String) pattern.get("signal")).isNotBlank();
            assertThat((String) pattern.get("mental_model")).isNotBlank();
            assertThat((String) pattern.get("invariant")).isNotBlank();
            assertThat(((String) pattern.get("recognition_rules")).length()).isGreaterThan(3);
            assertThat(((String) pattern.get("template")).length()).isGreaterThan(10);
        });
    }

    @Test
    @DisplayName("Twenty problems are seeded, two per pattern")
    void seedsTwentyProblemsAcrossEveryPattern() {
        List<Map<String, Object>> counts = jdbcTemplate.queryForList(
                "SELECT p.slug, count(*) AS problem_count FROM patterns p"
                        + " JOIN problems c ON c.primary_pattern_id = p.id"
                        + " GROUP BY p.id, p.slug, p.difficulty_order ORDER BY p.difficulty_order");

        assertThat(counts).hasSize(10);
        assertThat(counts).allSatisfy(row -> assertThat(((Number) row.get("problem_count")).intValue())
                .isEqualTo(2));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM problems", Integer.class)).isEqualTo(20);
    }

    @Test
    @DisplayName("Every problem satisfies the acceptance criteria of README section 92")
    void everyProblemIsComplete() {
        List<Map<String, Object>> problems = jdbcTemplate.queryForList("SELECT slug FROM problems ORDER BY slug");

        assertThat(problems).isNotEmpty();
        for (Map<String, Object> problem : problems) {
            String slug = (String) problem.get("slug");
            assertThat(incompleteProblems(slug))
                    .as("acceptance criteria for %s", slug)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("Every problem exposes a predict-the-move question and a text alternative per step")
    void everyProblemSupportsPredictTheMove() {
        List<Map<String, Object>> steps = jdbcTemplate.queryForList(
                "SELECT c.slug, count(*) FILTER (WHERE s.step_type = 'QUESTION') AS questions,"
                        + " count(*) AS total, count(*) FILTER (WHERE s.text IS NULL OR s.text = '') AS missing_text"
                        + " FROM problem_animation_steps s JOIN problems c ON c.id = s.problem_id"
                        + " GROUP BY c.slug");

        assertThat(steps).hasSize(20);
        assertThat(steps).allSatisfy(step -> {
            assertThat(((Number) step.get("questions")).intValue()).as("question steps").isGreaterThanOrEqualTo(1);
            assertThat(((Number) step.get("missing_text")).intValue()).as("steps without text").isZero();
        });
    }

    @Test
    @DisplayName("Every problem hides at least one test case and ships a Java reference solution")
    void everyProblemHasHiddenTestsAndASolution() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.slug, count(*) FILTER (WHERE t.is_hidden) AS hidden,"
                        + " count(*) AS cases, count(s.id) AS solutions"
                        + " FROM problems c"
                        + " JOIN problem_test_cases t ON t.problem_id = c.id"
                        + " LEFT JOIN problem_solutions s ON s.problem_id = c.id"
                        + " GROUP BY c.slug");

        assertThat(rows).hasSize(20);
        assertThat(rows).allSatisfy(row -> {
            assertThat(((Number) row.get("hidden")).intValue()).as("hidden cases").isGreaterThanOrEqualTo(1);
            assertThat(((Number) row.get("cases")).intValue()).as("total cases").isGreaterThanOrEqualTo(2);
            assertThat(((Number) row.get("solutions")).intValue()).as("solutions").isGreaterThanOrEqualTo(1);
        });
    }

    private List<String> incompleteProblems(String slug) {
        List<String> missing = new java.util.ArrayList<>();

        Integer problems = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM problems WHERE slug = ?", Integer.class, slug);
        if (problems == null || problems != 1) {
            missing.add("problem row");
        }

        Integer patternLinked = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM problems WHERE slug = ? AND primary_pattern_id IS NOT NULL",
                Integer.class, slug);
        if (patternLinked == null || patternLinked != 1) {
            missing.add("primary pattern");
        }

        Integer hintLevels = jdbcTemplate.queryForObject(
                "SELECT count(DISTINCT level) FROM problem_hints WHERE problem_id ="
                        + " (SELECT id FROM problems WHERE slug = ?)", Integer.class, slug);
        if (hintLevels == null || hintLevels < 5) {
            missing.add("hint ladder (levels " + hintLevels + ")");
        }

        Integer steps = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM problem_animation_steps WHERE problem_id ="
                        + " (SELECT id FROM problems WHERE slug = ?)", Integer.class, slug);
        if (steps == null || steps < 5) {
            missing.add("animation steps (" + steps + ")");
        }

        Integer content = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM problems WHERE slug = ? AND length(statement) > 40"
                        + " AND length(why_this_pattern) > 40 AND length(brute_force) > 40"
                        + " AND length(invariant) > 20 AND length(interview_explanation) > 60"
                        + " AND jsonb_array_length(pseudocode) >= 4"
                        + " AND jsonb_array_length(common_mistakes) >= 2"
                        + " AND jsonb_array_length(constraints) >= 2"
                        + " AND time_complexity IS NOT NULL AND space_complexity IS NOT NULL",
                Integer.class, slug);
        if (content == null || content != 1) {
            missing.add("teaching content");
        }

        return missing;
    }
}