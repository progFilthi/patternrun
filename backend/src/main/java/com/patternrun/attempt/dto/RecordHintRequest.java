package com.patternrun.attempt.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** One rung of the hint ladder. Recorded as it is revealed, so hint dependency is evidence. */
public record RecordHintRequest(@Min(1) @Max(5) int level) {
}
