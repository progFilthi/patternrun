package com.patternrun.attempt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The learner's code, from the editor.
 *
 * Stored, not evaluated. This endpoint is the autosave: it exists so a refresh does not cost a
 * learner their work, and it writes no outcome and no correctness, because nothing has been decided
 * when it is called.
 *
 * Execution lives on {@code /code/run} and {@code /code/submit}, which take their own request
 * record. Keeping the two apart means there is no endpoint that both accepts code and returns a
 * verdict, so nothing about a submission's correctness can be inferred from having been stored.
 *
 * {@code language} is still validated as JAVA|PYTHON because the column stores it and Phase 3
 * already recorded code that way; only Python can actually be executed.
 */
public record RecordCodeRequest(
        @NotBlank @Pattern(regexp = "JAVA|PYTHON", message = "must be JAVA or PYTHON") String language,
        @NotBlank @Size(max = 20000, message = "solution is too long") String code) {
}
