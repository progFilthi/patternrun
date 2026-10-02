package com.patternrun.problem.dto;

import com.patternrun.pattern.dto.PatternRef;
import com.patternrun.problem.Difficulty;
import com.patternrun.problem.TrainingDifficulty;
import java.util.List;
import java.util.UUID;

public record ProblemDetailResponse(
        UUID id,
        String slug,
        String title,
        Integer externalId,
        Difficulty difficulty,
        TrainingDifficulty trainingDifficulty,
        String statement,
        List<String> constraints,
        List<ExampleResponse> examples,
        PatternRef pattern,
        List<PatternRef> secondaryPatterns,
        List<String> pseudocode,
        ComplexityResponse complexity,
        String whyThisPattern,
        String bruteForce,
        String invariant,
        String interviewExplanation,
        List<String> commonMistakes,
        long hintCount,
        long animationStepCount,
        long visibleTestCaseCount,
        /**
         * The function the runner calls, or null when this problem is not available for coding yet.
         *
         * Null is the whole signal and it is a load-bearing one: the browser uses it to decide
         * whether to offer an editor at all, so the nineteen problems that have no structured test
         * arguments are honest about that instead of presenting an editor that cannot work.
         *
         * It is safe to send. The entrypoint is not a secret — the learner has to know what to call
         * — and knowing it does not help anyone pass, because the cases and the comparison are
         * server-side.
         */
        String runnableEntrypoint) {
}
