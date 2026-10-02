package com.patternrun.problem;

/**
 * What has to have happened before a hint is the right thing to show.
 *
 * This is the difference between a hint and a solution, and it is what keeps the coding stage from
 * degenerating into "press 1, press 2, press 3". A wrong answer can be explained by a value stored
 * in the wrong order. A timeout cannot be explained by anything except complexity. Offering the
 * first for the second teaches the learner to distrust the tool.
 *
 * {@code ANY} is the pre-Phase-4 behaviour and remains the default.
 */
public enum HintTrigger {
    WRONG_ANSWER,
    RUNTIME_ERROR,
    SYNTAX_ERROR,
    TIME_LIMIT_EXCEEDED,
    /** Nothing specific has gone wrong, or the learner has not started coding yet. */
    ANY
}