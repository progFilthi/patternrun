package com.patternrun.problem;

/**
 * How the runner turns a stored test case's arguments into a call.
 *
 * Most problems take their arguments literally and need nothing. Trees are the exception: the
 * content describes a binary tree the way it is written down everywhere --- level order, with nulls
 * for absent children --- and that is not something a function can be handed.
 *
 * <p>The alternative was making every learner who attempted a tree problem write their own
 * deserialisation, which is boilerplate that hides the algorithm the problem is actually about, and
 * which would read to them as their solution being broken.
 */
public enum ArgumentMode {

    /** Pass the stored arguments through as they are. */
    PLAIN,

    /** Build a binary tree from a level-order array and pass the root. */
    TREE
}
