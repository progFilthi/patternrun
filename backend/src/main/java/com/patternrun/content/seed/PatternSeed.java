package com.patternrun.content.seed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * One pattern file from {@code resources/seed/patterns}. Product content is version
 * controlled JSON (README section 65); this is the validated in memory form of it.
 */
public record PatternSeed(
        @NotBlank @Pattern(regexp = "[a-z0-9-]+") String slug,
        @NotBlank String name,
        @NotBlank String summary,
        @NotBlank String signal,
        @NotBlank String mentalModel,
        @NotEmpty List<@NotBlank String> recognitionRules,
        /** Lines of the canonical template; empty lines are intentional spacing. */
        @NotEmpty List<@NotNull String> template,
        @NotBlank String invariant,
        @Positive int difficultyOrder) {
}