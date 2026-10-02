package com.patternrun.problem;

/**
 * One question about what the problem is asking (README section 82, the reading step).
 *
 * {@code answerIndex} is the answer key and is never serialised into a response. The learner
 * sends an index and the server compares it here, for the same reason the predict-the-move
 * questions work that way: the answer already travels to the browser inside the animation
 * payload, so a client that reported its own correctness would be grading its own homework.
 */
public record BreakdownPrompt(
        String key,
        String prompt,
        java.util.List<String> options,
        Integer answerIndex,
        String explanation) {

    /** What the learner sees before answering: no answer, no explanation. */
    public record View(String key, String prompt, java.util.List<String> options) {
    }

    public View toView() {
        return new View(key, prompt, options);
    }
}
