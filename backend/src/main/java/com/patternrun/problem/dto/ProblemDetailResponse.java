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
        long visibleTestCaseCount) {
}