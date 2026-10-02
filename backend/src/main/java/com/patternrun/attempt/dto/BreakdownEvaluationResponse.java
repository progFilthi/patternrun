package com.patternrun.attempt.dto;

import java.util.List;

/**
 * How the learner read the problem.
 *
 * Returned after they commit, so the explanation can be shown alongside the verdict. This is the
 * only place the answer key is reflected back.
 *
 * {@code correct} is all-or-nothing across every prompt. A learner who identifies what they are
 * given but not what they must return has not read the problem, and the distinction is the whole
 * point of the step.
 */
public record BreakdownEvaluationResponse(
        boolean correct,
        int answered,
        int total,
        List<PromptResult> prompts) {

    public record PromptResult(
            String key,
            String prompt,
            int chosenIndex,
            boolean correct,
            String explanation) {
    }
}
