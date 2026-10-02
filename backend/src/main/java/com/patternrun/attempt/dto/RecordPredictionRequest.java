package com.patternrun.attempt.dto;

import jakarta.validation.constraints.Min;

/**
 * An answer to a predict-the-move question.
 *
 * Only the choice travels. Whether it was right is worked out server-side from the animation
 * step's own answerIndex, because that index is already inside the payload the browser received
 * and a client asserting its own correctness would be asserting something checkable.
 */
public record RecordPredictionRequest(@Min(0) int stepOrder, @Min(0) int chosenIndex) {
}
