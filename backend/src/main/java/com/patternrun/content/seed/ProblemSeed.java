package com.patternrun.content.seed;

import tools.jackson.databind.JsonNode;
import com.patternrun.problem.AnimationStepType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import com.patternrun.problem.Difficulty;
import com.patternrun.problem.ProgrammingLanguage;
import com.patternrun.problem.TrainingDifficulty;

/** One problem file from {@code resources/seed/problems}. */
public record ProblemSeed(
        @NotBlank @Pattern(regexp = "[a-z0-9-]+") String slug,
        @Positive Integer externalId,
        @NotBlank String title,
        @NotNull Difficulty difficulty,
        @NotNull TrainingDifficulty trainingDifficulty,
        @NotBlank String primaryPattern,
        List<@NotBlank String> secondaryPatterns,
        @NotBlank String statement,
        @NotEmpty List<@NotBlank String> constraints,
        @NotBlank String whyThisPattern,
        @NotBlank String bruteForce,
        @NotBlank String invariant,
        @NotEmpty List<@NotBlank String> pseudocode,
        @NotEmpty List<@NotBlank String> commonMistakes,
        @NotBlank String interviewExplanation,
        @NotBlank @Pattern(regexp = "O\\(.+\\)") String timeComplexity,
        @NotBlank @Pattern(regexp = "O\\(.+\\)") String spaceComplexity,
        @NotEmpty List<@Valid ExampleSeed> examples,
        @NotEmpty List<@Valid HintSeed> hints,
        @NotEmpty List<@Valid TestCaseSeed> testCases,
        @NotEmpty List<@Valid SolutionSeed> solutions,
        @NotEmpty List<@Valid AnimationStepSeed> animationSteps,
        /**
         * Optional. Only Two Sum carries one so far; a problem without a breakdown still starts
         * and completes, it just cannot earn the breakdown award.
         */
        List<@Valid BreakdownSeed> breakdown) {

    /** Hint ladder levels defined in README section 7. */
    public static final int HINT_LEVELS = 5;

    public record ExampleSeed(
            @NotBlank String input,
            @NotBlank String output,
            @NotBlank String explanation) {
    }

    public record HintSeed(
            @Min(1) @Max(HINT_LEVELS) int level,
            @NotBlank String content) {
    }

    public record TestCaseSeed(
            @NotBlank String label,
            @NotBlank String input,
            @NotBlank String expectedOutput,
            boolean hidden) {
    }

    public record SolutionSeed(
            @NotNull ProgrammingLanguage language,
            @NotBlank String code) {
    }

    /**
     * A generic animation frame: the renderer is chosen by {@code type}, the state is free
     * form JSON (README section 28). {@code text} is the accessible description every step
     * must carry (README section 56).
     */
    /**
     * One question about what the problem is asking.
     *
     * This is the step that exists to fix the thing that makes a problem hard to read rather
     * than hard to solve: a learner who has not established what they are given, what they must
     * return, and which constraint matters cannot recognise the signal, however well they know
     * the algorithms.
     *
     * {@code answerIndex} is content the server holds and never returns. The learner sends an
     * index, the server compares it here, exactly as the predict-the-move questions work.
     */
    public record BreakdownSeed(
            @NotBlank @Pattern(regexp = "[a-z0-9-]+") String key,
            @NotBlank String prompt,
            @NotEmpty List<@NotBlank String> options,
            // Zero is the first option, so @Positive would reject the most common answer there is.
            @NotNull @PositiveOrZero Integer answerIndex,
            @NotBlank String explanation) {
    }

    public record AnimationStepSeed(
            @Positive int order,
            @NotNull AnimationStepType type,
            @NotBlank String title,
            @NotBlank String description,
            @NotBlank String text,
            @NotNull JsonNode payload) {
    }
}