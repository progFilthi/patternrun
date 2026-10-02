package com.patternrun.execution;

import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.problem.ProgrammingLanguage;

/**
 * Runs a learner's program against cases and reports what happened.
 *
 * The seam that keeps execution swappable. Python today; Java, JavaScript or anything else later is
 * a second implementation and a dispatch line in {@link CodeExecutionService}, with no change to the
 * training domain that calls it. Nothing above this interface knows what a container is, and that
 * is what lets the domain be tested with a stub instead of a runtime.
 *
 * Implementations must be honest about failure. An {@link ExecutionOutcome#INTERNAL_ERROR} means the
 * runner broke, not that the learner's code is wrong, and conflating the two would tell someone
 * their correct solution does not work.
 */
public interface CodeExecutionProvider {

    /** The language this provider handles. */
    ProgrammingLanguage language();

    /**
     * Runs the program and reports what happened.
     *
     * Never throws for anything the learner's code can do. A syntax error, a crash, an infinite
     * loop and an out-of-memory condition are all results, and the caller decides how to describe
     * them. An implementation that cannot run at all should return
     * {@link ExecutionReport#infrastructureFailure}.
     */
    ExecutionReport execute(ExecutionRequest request);

    /** Whether this provider is usable right now. */
    boolean isAvailable();

    /** Thrown when a problem has no runnable cases, which is a content gap rather than a fault. */
    class NotRunnableException extends ResourceNotFoundException {
        public NotRunnableException(String message) {
            super(message);
        }
    }
}