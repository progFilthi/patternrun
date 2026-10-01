package com.patternrun.pattern.dto;

import java.util.UUID;

public record PatternSummaryResponse(
        UUID id,
        String slug,
        String name,
        String signal,
        int difficultyOrder,
        long problemCount) {
}