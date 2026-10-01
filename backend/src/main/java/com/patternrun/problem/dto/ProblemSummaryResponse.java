package com.patternrun.problem.dto;

import com.patternrun.pattern.dto.PatternRef;
import com.patternrun.problem.Difficulty;
import com.patternrun.problem.TrainingDifficulty;
import java.util.UUID;

public record ProblemSummaryResponse(
        UUID id,
        String slug,
        String title,
        Integer externalId,
        Difficulty difficulty,
        TrainingDifficulty trainingDifficulty,
        PatternRef pattern,
        ComplexityResponse complexity) {
}