package com.patternrun.execution;

import java.util.List;

/**
 * What the provider reports back, before anything has been decided about how much of it the
 * learner gets to see.
 *
 * Kept separate from the response record on purpose. The provider knows about hidden cases because
 * it has to run them; the response knows which of them may be described. If those were one type,
 * adding a field to the provider result would quietly start leaking it.
 *
 * <p>{@code caseOutcomes} is positional and matches the cases that were sent, in order. It exists
 * because a total is not enough: with cases [pass, fail, pass] the count is two, and deriving
 * per-case verdicts from a count would report the third case as failed when it passed. The learner
 * would be told their code disagrees with itself.
 *
 * @param stderr captured for diagnostics only. Never sent to the browser: it can contain the
 *     container's own noise, and a learner should never be shown our plumbing.
 */
public record ExecutionReport(
        ExecutionOutcome outcome,
        List<CaseOutcome> caseOutcomes,
        Long durationMs,
        String errorType,
        String errorMessage,
        Integer errorLine,
        String stdout,
        String stderr) {

    /**
     * One case's result.
     *
     * @param actual what the code returned, already rendered for display. Null when the case
     *     raised, because there was no value to render.
     * @param failure set when this specific case threw, which is what separates "returned the wrong
     *     thing" from "never got far enough to return anything".
     */
    public record CaseOutcome(boolean passed, String actual, ExecutionOutcome failure) {

        public static CaseOutcome passed(String actual) {
            return new CaseOutcome(true, actual, null);
        }

        public static CaseOutcome wrong(String actual) {
            return new CaseOutcome(false, actual, null);
        }

        public static CaseOutcome threw(ExecutionOutcome failure) {
            return new CaseOutcome(false, null, failure);
        }
    }

    public int casesTotal() {
        return caseOutcomes.size();
    }

    public int casesPassed() {
        return (int) caseOutcomes.stream().filter(CaseOutcome::passed).count();
    }

    /** The failure to show. */
    public static ExecutionReport infrastructureFailure(String message) {
        return new ExecutionReport(
                ExecutionOutcome.INTERNAL_ERROR, List.of(), null, "RunnerUnavailable", message, null, null, null);
    }
}