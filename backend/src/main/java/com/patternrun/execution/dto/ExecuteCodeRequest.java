package com.patternrun.execution.dto;

import com.patternrun.problem.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Source code from the editor, and nothing else.
 *
 * The shape of this record is the security boundary in one place. There is no correctness field, no
 * passed flag, no expected output and no claim about what the code will do, because the server is
 * going to run it and find out. A request that wanted to assert its own success has nowhere to put
 * the assertion: Jackson drops unknown properties, so {@code passed: true} is silently discarded
 * rather than honoured.
 *
 * The length cap is enforced here and again in the runner. Enforcing it at the edge stops a
 * megabyte of source from being queued for execution at all.
 */
public record ExecuteCodeRequest(
        @NotBlank @Pattern(regexp = "PYTHON", message = "only PYTHON can be executed today")
        String language,
        @NotBlank @Size(max = 20000, message = "solution is too long")
        String code) {

    public ProgrammingLanguage languageOrThrow() {
        return ProgrammingLanguage.valueOf(language);
    }
}