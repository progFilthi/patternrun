package com.patternrun.pattern.dto;

import java.util.List;
import java.util.UUID;

/** Lightweight reference to a pattern, embedded in problem responses. */
public record PatternRef(UUID id, String slug, String name) {
}