package com.patternrun.execution;

/**
 * Why code was run.
 *
 * The two are different acts and the difference is load-bearing. A {@link #RUN} is the learner
 * checking their work against the examples they were already shown, so it can never make a problem
 * solved no matter how well it goes. A {@link #SUBMIT} is the evaluation set, hidden cases
 * included, and only its {@link ExecutionOutcome#ACCEPTED} counts.
 *
 * Collapsing them would be the easy mistake: it would make "Run" a completion in disguise, and a
 * learner could finish a problem by pressing the same button they press while thinking.
 */
public enum ExecutionKind {

    /** Tests against the visible examples. Never decides that a problem is solved. */
    RUN,

    /** Evaluates the backend-controlled case set. This is what solving means. */
    SUBMIT
}