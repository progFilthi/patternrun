package com.patternrun.problem;

/**
 * Which part of the loop a hint belongs to.
 *
 * A hint is only useful if it answers the question the learner is actually asking. Someone
 * reasoning about the problem needs "what should I remember while scanning"; someone staring at a
 * failing test needs "check what you stored before moving on". Serving the first to the second is
 * not a slower hint, it is a wrong one.
 *
 * {@code ANY} is the pre-Phase-4 behaviour and remains the default, so a problem that has not been
 * given a staged ladder keeps working exactly as it did.
 */
public enum HintStage {
    /** Before any reasoning: the reading step. */
    BREAKDOWN,
    /** Understanding the problem, before the editor. */
    REASONING,
    /** Writing, running and debugging code. */
    CODING,
    /** Applicable whenever the ladder is reached. */
    ANY
}