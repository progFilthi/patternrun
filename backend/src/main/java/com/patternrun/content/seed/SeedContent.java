package com.patternrun.content.seed;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** All seed content in memory: the single source of truth for Phase 1 problems and patterns. */
public record SeedContent(
        @NotEmpty List<@Valid PatternSeed> patterns,
        @NotEmpty List<@Valid ProblemSeed> problems) {

    public List<String> patternSlugs() {
        return patterns.stream().map(PatternSeed::slug).toList();
    }
}