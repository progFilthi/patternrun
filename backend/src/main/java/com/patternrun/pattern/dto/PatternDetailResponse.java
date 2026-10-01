package com.patternrun.pattern.dto;

import java.util.List;
import java.util.UUID;

public record PatternDetailResponse(
        UUID id,
        String slug,
        String name,
        String summary,
        String signal,
        String mentalModel,
        List<String> recognitionRules,
        List<String> template,
        String invariant,
        int difficultyOrder,
        long problemCount) {
}