package com.patternrun.problem.dto;

import com.patternrun.pattern.dto.PatternRef;

public record ExampleResponse(int ordinal, String input, String output, String explanation) {
}