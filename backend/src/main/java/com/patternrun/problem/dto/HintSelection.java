package com.patternrun.problem.dto;

/**
 * One rung, chosen for the moment the learner is in.
 *
 * Carries the stage and trigger it was selected for so the client can record where it came from.
 * The server still derives whether revealing it mattered; all the browser does with this is show
 * the text and report which rung was read.
 */
public record HintSelection(
        int level,
        String content,
        String stage,
        String trigger) {
}