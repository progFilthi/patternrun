package com.patternrun.execution;

import tools.jackson.databind.JsonNode;

/**
 * One case to run, as the runner receives it.
 *
 * Carries the structured arguments and the expected value rather than the display strings, because
 * the display strings are prose for a human and parsing them is how "-3" turns into a subtraction.
 *
 * {@code hidden} travels this far so the service can decide what to say afterwards, and no further:
 * nothing downstream of the runner can see a hidden case's label, input, expected value or output.
 */
public record ExecutionCase(String label, JsonNode args, JsonNode expected, boolean hidden) {

    ExecutionCase asHidden() {
        return new ExecutionCase(label, args, expected, true);
    }
}