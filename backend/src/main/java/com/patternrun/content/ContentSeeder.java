package com.patternrun.content;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.patternrun.content.seed.PatternSeed;
import com.patternrun.content.seed.ProblemSeed;
import com.patternrun.content.seed.SeedContent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads validated seed content into the database on startup.
 *
 * Idempotent: when the content tables already hold rows the pass is skipped, so restarting
 * the API never duplicates content. A changed content file therefore needs a clean database
 * ({@code docker compose down -v}) rather than a silent overwrite, which keeps this a loader
 * and not an editor.
 */
@Component
public class ContentSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentSeeder.class);

    private final SeedContentReader contentReader;
    private final ContentSeedProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ContentSeeder(SeedContentReader contentReader,
                         ContentSeedProperties properties,
                         JdbcTemplate jdbcTemplate,
                         ObjectMapper objectMapper) {
        this.contentReader = contentReader;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Content seeding is disabled");
            return;
        }
        seed();
    }

    /** Public so tests can prove the pass is idempotent. */
    @Transactional
    public void seed() {
        if (isAlreadySeeded()) {
            log.info("Content already present, skipping the seed pass");
            return;
        }

        SeedContent content = contentReader.read();
        Map<String, UUID> patternIds = insertPatterns(content.patterns());
        for (ProblemSeed problem : content.problems()) {
            insertProblem(patternIds, problem);
        }
        log.info("Seeded {} patterns and {} problems", content.patterns().size(), content.problems().size());
    }

    private boolean isAlreadySeeded() {
        Integer patterns = jdbcTemplate.queryForObject("SELECT count(*) FROM patterns", Integer.class);
        return patterns != null && patterns > 0;
    }

    private Map<String, UUID> insertPatterns(Iterable<PatternSeed> patterns) {
        Map<String, UUID> patternIds = new LinkedHashMap<>();
        for (PatternSeed pattern : patterns) {
            UUID id = jdbcTemplate.queryForObject("""
                            INSERT INTO patterns (slug, name, summary, signal, mental_model, recognition_rules,
                                                  template, invariant, difficulty_order)
                            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                            RETURNING id
                            """,
                    UUID.class,
                    pattern.slug(),
                    pattern.name(),
                    pattern.summary(),
                    pattern.signal(),
                    pattern.mentalModel(),
                    json(pattern.recognitionRules()),
                    json(pattern.template()),
                    pattern.invariant(),
                    pattern.difficultyOrder());
            patternIds.put(pattern.slug(), id);
        }
        return patternIds;
    }

    private void insertProblem(Map<String, UUID> patternIds, ProblemSeed problem) {
        UUID problemId = jdbcTemplate.queryForObject("""
                        INSERT INTO problems (external_id, slug, title, difficulty, training_difficulty, statement,
                                              constraints, why_this_pattern, brute_force, invariant, pseudocode,
                                              common_mistakes, interview_explanation, time_complexity,
                                              space_complexity, primary_pattern_id, breakdown, entrypoint,
                                              argument_mode)
                        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?::jsonb, ?, ?)
                        RETURNING id
                        """,
                UUID.class,
                problem.externalId(),
                problem.slug(),
                problem.title(),
                problem.difficulty().name(),
                problem.trainingDifficulty().name(),
                problem.statement(),
                json(problem.constraints()),
                problem.whyThisPattern(),
                problem.bruteForce(),
                problem.invariant(),
                json(problem.pseudocode()),
                json(problem.commonMistakes()),
                problem.interviewExplanation(),
                problem.timeComplexity(),
                problem.spaceComplexity(),
                patternIds.get(problem.primaryPattern()),
                json(toBreakdownPrompts(problem.breakdown())),
                problem.entrypoint(),
                problem.argumentModeOrPlain().name());

        insertSecondaryPatterns(patternIds, problem, problemId);
        insertExamples(problem, problemId);
        insertHints(problem, problemId);
        insertAnimationSteps(problem, problemId);
        insertTestCases(problem, problemId);
        insertSolutions(problem, problemId);
    }

    /**
     * Maps the seed records onto the stored shape.
     *
     * A problem with no breakdown seeds an empty list rather than null, so the column's NOT NULL
     * holds and the read endpoint can treat "no breakdown" as "nothing to ask" instead of
     * branching on absence.
     */
    private static List<Map<String, Object>> toBreakdownPrompts(List<ProblemSeed.BreakdownSeed> breakdown) {
        if (breakdown == null) {
            return List.of();
        }
        return breakdown.stream()
                .map(prompt -> Map.<String, Object>of(
                        "key", prompt.key(),
                        "prompt", prompt.prompt(),
                        "options", prompt.options(),
                        "answerIndex", prompt.answerIndex(),
                        "explanation", prompt.explanation()))
                .toList();
    }

    private void insertSecondaryPatterns(Map<String, UUID> patternIds, ProblemSeed problem, UUID problemId) {
        for (String secondary : problem.secondaryPatterns()) {
            jdbcTemplate.update("INSERT INTO problem_secondary_patterns (problem_id, pattern_id) VALUES (?, ?)",
                    problemId, patternIds.get(secondary));
        }
    }

    private void insertExamples(ProblemSeed problem, UUID problemId) {
        int ordinal = 1;
        for (ProblemSeed.ExampleSeed example : problem.examples()) {
            jdbcTemplate.update("""
                            INSERT INTO problem_examples (problem_id, ordinal, input, output, explanation)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    problemId, ordinal++, example.input(), example.output(), example.explanation());
        }
    }

    private void insertHints(ProblemSeed problem, UUID problemId) {
        for (ProblemSeed.HintSeed hint : problem.hints()) {
            jdbcTemplate.update("""
                            INSERT INTO problem_hints (problem_id, level, stage, trigger, content)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    problemId, hint.level(), hint.stageOrAny().name(), hint.triggerOrAny().name(),
                    hint.content());
        }
    }

    private void insertAnimationSteps(ProblemSeed problem, UUID problemId) {
        for (ProblemSeed.AnimationStepSeed step : problem.animationSteps()) {
            jdbcTemplate.update("""
                            INSERT INTO problem_animation_steps
                            (problem_id, step_order, step_type, title, description, text, payload)
                            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                            """,
                    problemId, step.order(), step.type().name(), step.title(), step.description(), step.text(),
                    step.payload().toString());
        }
    }

    /**
     * Writes the display columns and, when the case is runnable, the machine-readable ones.
     *
     * Both forms are stored rather than derived. The display string is what the learner is shown
     * and has always been authoritative for that; the JSONB pair is what a runner calls. Keeping
     * them side by side means adding an editor changed no part of how content reads.
     */
    private void insertTestCases(ProblemSeed problem, UUID problemId) {
        int ordinal = 1;
        for (ProblemSeed.TestCaseSeed testCase : problem.testCases()) {
            jdbcTemplate.update("""
                            INSERT INTO problem_test_cases
                            (problem_id, ordinal, label, input_data, expected_output, is_hidden, call, expected_json)
                            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
                            """,
                    problemId, ordinal++, testCase.label(), testCase.input(), testCase.expectedOutput(),
                    testCase.hidden(),
                    testCase.call() == null ? null : testCase.call().toString(),
                    testCase.expected() == null ? null : testCase.expected().toString());
        }
    }

    private void insertSolutions(ProblemSeed problem, UUID problemId) {
        for (ProblemSeed.SolutionSeed solution : problem.solutions()) {
            jdbcTemplate.update("INSERT INTO problem_solutions (problem_id, language, code) VALUES (?, ?, ?)",
                    problemId, solution.language().name(), solution.code());
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new SeedContentException("Cannot serialize seed content", ex);
        }
    }
}