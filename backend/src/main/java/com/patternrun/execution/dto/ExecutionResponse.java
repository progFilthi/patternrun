package com.patternrun.execution.dto;

import java.util.List;

/**
 * What one execution produced, and what the learner is allowed to be told about it.
 *
 * <h2>Hidden cases</h2>
 *
 * A hidden case contributes its {@code passed} boolean and nothing else. Label, input, expected
 * value and actual value are all {@code null}, and {@code hidden} is {@code true}. A learner is
 * told how many of the hidden cases passed and nothing about which, because "5 of 5 hidden passed"
 * is the whole useful signal; a label like "Two zeros" would hand over the test set.
 *
 * <h2>Who decided {@code outcome}</h2>
 *
 * The server, by running the code. The request carried the source and nothing else, and there is no
 * field on this record a client could have set to make itself accepted.
 *
 * @param kind whether this was a Run against the visible examples or a Submit against the
 *     evaluation set. It is in the response rather than inferred, because the difference between
 *     "2 of 3 public tests passed" and "wrong answer" is the single most important thing the UI
 *     shows and it must not be a guess.
 */
public record ExecutionResponse(
        String problemSlug,
        String kind,
        String outcome,
        int casesTotal,
        int casesPassed,
        Long durationMs,
        List<CaseResult> cases,
        ErrorDetail error) {

    /**
     * One case.
     *
     * @param passed whether this case passed.
     * @param actual what the learner's code returned, already rendered for display. Null when the
     *     case threw, because there was no value.
     */
    public record CaseResult(
            int ordinal,
            String label,
            String input,
            String expected,
            String actual,
            boolean passed,
            boolean hidden) {
    }

    /**
     * A failure, in words a learner can act on.
     *
     * @param type the exception name, e.g. {@code TypeError}. Present so the UI can lead with it.
     * @param line the line in <em>their</em> file. Never a path: the sandbox's own file layout is
     *     both noise and a small amount of information about the infrastructure.
     * @param message already scrubbed of anything the learner should not see.
     */
    public record ErrorDetail(String type, Integer line, String message) {
    }
}