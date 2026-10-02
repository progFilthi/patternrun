package com.patternrun.problem.dto;

import com.patternrun.problem.BreakdownPrompt;
import java.util.List;

/**
 * The breakdown prompts for a problem.
 *
 * There is deliberately no answer index and no explanation here. The learner gets the question
 * and the options; the server keeps the key and hands over the explanation only after they have
 * committed. Serving the key would make the whole step decorative, because the answer is already
 * in the payload the browser receives for the predict-the-move questions.
 */
public record BreakdownResponse(String problemSlug, List<BreakdownPrompt.View> prompts) {

    public static BreakdownResponse of(String slug, List<BreakdownPrompt> prompts) {
        return new BreakdownResponse(slug, prompts.stream().map(BreakdownPrompt::toView).toList());
    }
}
