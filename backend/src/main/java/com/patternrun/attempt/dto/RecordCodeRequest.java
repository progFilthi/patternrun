package com.patternrun.attempt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The learner's code, from the editor.
 *
 * Stored, not executed. README section 66 forbids running arbitrary code inside this API, so
 * execution happens in the learner's own browser (Python via Pyodide) and Java waits for an
 * isolated judge. Nothing here is ever evaluated on the server.
 */
public record RecordCodeRequest(
        @NotBlank @Pattern(regexp = "JAVA|PYTHON", message = "must be JAVA or PYTHON") String language,
        @NotBlank @Size(max = 20000, message = "solution is too long") String code) {
}
