package com.patternrun.content.seed;

import tools.jackson.databind.JsonNode;
import com.patternrun.problem.AnimationStepType;
import com.patternrun.problem.ArgumentMode;
import com.patternrun.problem.HintStage;
import com.patternrun.problem.HintTrigger;
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
        List<@Valid BreakdownSeed> breakdown,
        /**
         * Optional. The function a runner calls. Absent means the problem is content-only and no
         * editor is offered.
         */
        @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_]*") String entrypoint,
        /**
         * Optional. How the runner turns stored arguments into a call. Absent means {@code PLAIN},
         * which is every problem whose arguments are already the values its function wants.
         *
         * {@code TREE} is for the two problems whose data is a level-order array rather than the
         * object the entrypoint takes, so that a learner is never asked to write deserialisation
         * around the algorithm they are being asked about.
         */
        ArgumentMode argumentMode) {

    public ArgumentMode argumentModeOrPlain() {
        return argumentMode == null ? ArgumentMode.PLAIN : argumentMode;
    }

    /** Hint ladder levels defined in README section 7. */
    public static final int HINT_LEVELS = 5;

    public record ExampleSeed(
            @NotBlank String input,
            @NotBlank String output,
            @NotBlank String explanation) {
    }

    public record HintSeed(
            @Min(1) @Max(HINT_LEVELS) int level,
            /**
             * Optional. Absent means {@code ANY}, which is every hint authored before staging
             * existed and keeps those problems working unchanged.
             */
            HintStage stage,
            /** Optional. Absent means {@code ANY}. */
            HintTrigger trigger,
            @NotBlank String content) {

        public HintStage stageOrAny() {
            return stage == null ? HintStage.ANY : stage;
        }

        public HintTrigger triggerOrAny() {
            return trigger == null ? HintTrigger.ANY : trigger;
        }

        /** Whether this rung belongs to the ladder shown before the editor. */
        public boolean isReasoningRung() {
            HintStage resolved = stageOrAny();
            return (resolved == HintStage.ANY || resolved == HintStage.REASONING)
                    && triggerOrAny() == HintTrigger.ANY;
        }
    }

    /**
     * One test case.
     *
     * {@code input} and {@code expectedOutput} are prose for a human. {@code call} and
     * {@code expected} are the runnable form, and they are separate on purpose: parsing
     * "nums = [2,7,11,15], target = 9" to recover the arguments would make
     * {@code [-3, 4]} ambiguous with a subtraction and would break the first problem with a
     * string in it.
     *
     * Both are absent for the nineteen problems that are not runnable yet, which is exactly how
     * the API says so.
     */
    public record TestCaseSeed(
            @NotBlank String label,
            @NotBlank String input,
            @NotBlank String expectedOutput,
            boolean hidden,
            /** Positional arguments for the entrypoint. Absent means display-only. */
            JsonNode call,
            /** Expected return value, compared structurally. Absent means display-only. */
            JsonNode expected) {

        /** Whether the runner can execute this case. */
        public boolean isRunnable() {
            return call != null && expected != null;
        }
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