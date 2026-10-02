package com.patternrun.content;

import tools.jackson.databind.ObjectMapper;
import com.patternrun.content.seed.PatternSeed;
import com.patternrun.content.seed.ProblemSeed;
import com.patternrun.content.seed.SeedContent;
import com.patternrun.problem.AnimationStepType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Reads the version controlled content in {@code resources/seed} and validates it before
 * anything touches the database.
 *
 * Two layers: Bean Validation on the records, then the cross field rules only the whole file
 * can satisfy (unique slugs, known pattern references, a complete five level hint ladder, at
 * least one hidden test case, a Java reference solution, at least one predict-the-move
 * question). Invalid content fails startup with a precise message instead of reaching the
 * database.
 */
@Component
public class SeedContentReader {

    private static final Logger log = LoggerFactory.getLogger(SeedContentReader.class);

    private static final String PATTERN_LOCATION = "classpath*:seed/patterns/*.json";
    private static final String PROBLEM_LOCATION = "classpath*:seed/problems/*.json";

    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public SeedContentReader(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public SeedContent read() {
        List<PatternSeed> patterns = readAll(PATTERN_LOCATION, PatternSeed.class);
        List<ProblemSeed> problems = readAll(PROBLEM_LOCATION, ProblemSeed.class);

        SeedContent content = new SeedContent(
                patterns.stream().sorted(Comparator.comparingInt(PatternSeed::difficultyOrder)).toList(),
                problems.stream().sorted(Comparator.comparing(ProblemSeed::externalId)).toList());

        validate(content);
        log.info("Read {} patterns and {} problems from {}", patterns.size(), problems.size(), "resources/seed");
        return content;
    }

    private <T> List<T> readAll(String locationPattern, Class<T> type) {
        Resource[] found;
        try {
            found = resourceResolver.getResources(locationPattern);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read seed content from " + locationPattern, ex);
        }
        if (found.length == 0) {
            throw new SeedContentException("No seed content found at " + locationPattern);
        }
        List<Resource> resources = new ArrayList<>(List.of(found));
        resources.sort(Comparator.comparing(Resource::getFilename));

        List<T> content = new ArrayList<>(resources.size());
        for (Resource resource : resources) {
            try {
                content.add(objectMapper.readValue(resource.getInputStream(), type));
            } catch (IOException ex) {
                throw new SeedContentException(
                        "Cannot parse " + resource.getFilename() + ": " + ex.getMessage(), ex);
            }
        }
        return content;
    }

    private void validate(SeedContent content) {
        validateBeanConstraints(content);
        validatePatternSlugs(content);
        validateProblemIdentity(content);
        for (ProblemSeed problem : content.problems()) {
            validatePatternReferences(content, problem);
            validateHintLadder(problem);
            validateTestCasesAndSolution(problem);
            validateAnimationSteps(problem);
            validateBreakdown(problem);
        }
    }

    private void validateBeanConstraints(SeedContent content) {
        Set<ConstraintViolation<SeedContent>> violations = validator.validate(content);
        if (violations.isEmpty()) {
            return;
        }
        String detail = violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .reduce((left, right) -> left + "; " + right)
                .orElseThrow();
        throw new SeedContentException("Seed content violates the content contract: " + detail);
    }

    private void validatePatternSlugs(SeedContent content) {
        Set<String> slugs = new HashSet<>();
        for (PatternSeed pattern : content.patterns()) {
            if (!slugs.add(pattern.slug())) {
                throw new SeedContentException("Duplicate pattern slug: " + pattern.slug());
            }
        }
    }

    private void validateProblemIdentity(SeedContent content) {
        Set<String> slugs = new HashSet<>();
        Set<Integer> externalIds = new HashSet<>();
        for (ProblemSeed problem : content.problems()) {
            if (!slugs.add(problem.slug())) {
                throw new SeedContentException("Duplicate problem slug: " + problem.slug());
            }
            if (!externalIds.add(problem.externalId())) {
                throw new SeedContentException("Duplicate externalId: " + problem.externalId());
            }
        }
    }

    private void validatePatternReferences(SeedContent content, ProblemSeed problem) {
        Set<String> patternSlugs = new HashSet<>(content.patternSlugs());
        if (!patternSlugs.contains(problem.primaryPattern())) {
            throw new SeedContentException(
                    "Problem " + problem.slug() + " references unknown pattern: " + problem.primaryPattern());
        }
        for (String secondary : problem.secondaryPatterns()) {
            if (!patternSlugs.contains(secondary)) {
                throw new SeedContentException(
                        "Problem " + problem.slug() + " references unknown pattern: " + secondary);
            }
        }
    }

    private void validateHintLadder(ProblemSeed problem) {
        Set<Integer> levels = new HashSet<>();
        for (ProblemSeed.HintSeed hint : problem.hints()) {
            if (!levels.add(hint.level())) {
                throw new SeedContentException(
                        "Duplicate hint level " + hint.level() + " in " + problem.slug());
            }
        }
        for (int level = 1; level <= ProblemSeed.HINT_LEVELS; level++) {
            if (!levels.contains(level)) {
                throw new SeedContentException("Missing hint level " + level + " in " + problem.slug());
            }
        }
    }

    private void validateTestCasesAndSolution(ProblemSeed problem) {
        boolean hasHiddenCase = problem.testCases().stream().anyMatch(ProblemSeed.TestCaseSeed::hidden);
        if (!hasHiddenCase) {
            throw new SeedContentException(
                    "Problem " + problem.slug() + " needs at least one hidden test case");
        }
        boolean hasJavaSolution = problem.solutions().stream()
                .anyMatch(solution -> solution.language().name().equals("JAVA"));
        if (!hasJavaSolution) {
            throw new SeedContentException("Problem " + problem.slug() + " needs a JAVA reference solution");
        }
    }

    private void validateAnimationSteps(ProblemSeed problem) {
        Set<Integer> orders = new HashSet<>();
        boolean hasQuestion = false;
        for (ProblemSeed.AnimationStepSeed step : problem.animationSteps()) {
            if (!orders.add(step.order())) {
                throw new SeedContentException(
                        "Duplicate animation step " + step.order() + " in " + problem.slug());
            }
            hasQuestion = hasQuestion || step.type() == AnimationStepType.QUESTION;
        }
        if (!hasQuestion) {
            throw new SeedContentException("Problem " + problem.slug()
                    + " needs a QUESTION step (predict the move, README section 8)");
        }
    }

    /**
     * A breakdown is only worth asking if every prompt has a real answer to check against.
     *
     * An out-of-range or missing {@code answerIndex} would make a prompt permanently
     * unanswerable, and it would do so silently: the learner would pick something, be told they
     * were wrong, and have no way to know the content was broken. That is the worst possible
     * failure for a step whose whole job is teaching reading, so it fails startup instead.
     */
    private void validateBreakdown(ProblemSeed problem) {
        List<ProblemSeed.BreakdownSeed> breakdown = problem.breakdown();
        if (breakdown == null || breakdown.isEmpty()) {
            return;
        }

        Set<String> keys = new HashSet<>();
        for (ProblemSeed.BreakdownSeed prompt : breakdown) {
            if (!keys.add(prompt.key())) {
                throw new SeedContentException(
                        "Duplicate breakdown key " + prompt.key() + " in " + problem.slug());
            }
            if (prompt.options().size() < 2) {
                throw new SeedContentException(
                        "Breakdown " + prompt.key() + " in " + problem.slug() + " needs two options");
            }
            if (prompt.answerIndex() == null
                    || prompt.answerIndex() < 0
                    || prompt.answerIndex() >= prompt.options().size()) {
                throw new SeedContentException(
                        "Breakdown " + prompt.key() + " in " + problem.slug()
                                + " has no answer among its options");
            }
        }
    }
}