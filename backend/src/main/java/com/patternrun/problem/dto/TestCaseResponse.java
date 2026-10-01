package com.patternrun.problem.dto;

public record TestCaseResponse(int ordinal, String label, String input, String expectedOutput) {
}