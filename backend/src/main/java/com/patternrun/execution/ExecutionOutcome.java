package com.patternrun.execution;

/**
 * What happened when code was run.
 *
 * Named for what the learner is told, not for what the runner saw. The distinction that matters
 * most is {@link #WRONG_ANSWER} versus {@link #RUNTIME_ERROR}: one says "your idea did not hold"
 * and the other says "your code never got far enough to have an idea". Collapsing them into a
 * single "failed" would be the difference between a hint that helps and a hint that misleads.
 *
 * {@link #ACCEPTED} means the backend ran the code against the backend-controlled evaluation set
 * and every case passed. It is the only value that means a problem is solved, and only the server
 * can produce it.
 */
public enum ExecutionOutcome {

    /** Every evaluation case passed. */
    ACCEPTED,

    /** Ran to completion, returned something, and it was not what was expected. */
    WRONG_ANSWER,

    /** Ran and threw. The learner's bug, reported with a line number. */
    RUNTIME_ERROR,

    /** Exceeded the per-case time limit. A complexity problem, not a correctness one. */
    TIME_LIMIT_EXCEEDED,

    /** Exceeded the memory limit. */
    MEMORY_LIMIT_EXCEEDED,

    /** Could not be parsed, so nothing ran. */
    SYNTAX_ERROR,

    /** The runner itself failed. Never the learner's fault, and never their code's. */
    INTERNAL_ERROR;

    public boolean isAccepted() {
        return this == ACCEPTED;
    }

    /** Whether the failure is about the learner's code rather than about our infrastructure. */
    public boolean isLearnerFault() {
        return switch (this) {
            case ACCEPTED, INTERNAL_ERROR -> false;
            default -> true;
        };
    }
}