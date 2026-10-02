package com.patternrun.execution;

import com.patternrun.problem.ArgumentMode;
import com.patternrun.problem.ProgrammingLanguage;
import java.util.List;

/**
 * Everything a runner needs, and nothing it does not.
 *
 * Notably absent: the problem, the learner, the attempt, and the expected answers in any form the
 * runner could not already get from the cases. A provider that can see the learner's id would
 * eventually log it; a provider that can see a hidden case's label could leak it into an error
 * message. This record is the whole contract.
 *
 * @param language only Python has a provider today. The field exists so adding one is a new
 *     implementation of {@link CodeExecutionProvider} rather than a change to everything around it.
 */
public record ExecutionRequest(
        ProgrammingLanguage language,
        String source,
        String entrypoint,
        List<ExecutionCase> cases,
        double timeoutSeconds,
        ArgumentMode argumentMode) {
}