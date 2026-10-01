package com.patternrun.problem.dto;

import tools.jackson.databind.JsonNode;
import com.patternrun.problem.AnimationStepType;

/** One frame of the animation engine: type + payload + text alternative. */
public record AnimationStepResponse(
        int order,
        AnimationStepType type,
        String title,
        String description,
        String text,
        JsonNode payload) {
}