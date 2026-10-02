package com.patternrun.problem;

import com.patternrun.common.PageResponse;
import com.patternrun.problem.dto.AnimationStepResponse;
import com.patternrun.problem.dto.BreakdownResponse;
import com.patternrun.problem.dto.HintResponse;
import com.patternrun.problem.dto.ProblemDetailResponse;
import com.patternrun.problem.dto.ProblemSummaryResponse;
import com.patternrun.problem.dto.TestCaseResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problems")
@Validated
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public PageResponse<ProblemSummaryResponse> findAll(
            @RequestParam(required = false) @Pattern(regexp = "[a-z0-9-]+") String pattern,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "externalId") @Pattern(regexp = "[a-zA-Z]+") String sort) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sort));
        return PageResponse.from(problemService.findPage(pattern, difficulty, pageRequest));
    }

    @GetMapping("/{slug}")
    public ProblemDetailResponse findBySlug(@PathVariable String slug) {
        return problemService.findBySlug(slug);
    }

    /**
     * The prompts that break the statement down into what is given, what is asked and which
     * constraint matters.
     *
     * Returns no answer index and no explanation. Those come back from the attempt endpoint once
     * the learner has committed.
     */
    @GetMapping("/{slug}/breakdown")
    public BreakdownResponse breakdown(@PathVariable String slug) {
        return problemService.findBreakdown(slug);
    }

    @GetMapping("/{slug}/hints")
    public List<HintResponse> findHints(@PathVariable String slug) {
        return problemService.findHints(slug);
    }

    @GetMapping("/{slug}/animation")
    public List<AnimationStepResponse> findAnimation(@PathVariable String slug) {
        return problemService.findAnimation(slug);
    }

    @GetMapping("/{slug}/test-cases")
    public List<TestCaseResponse> findTestCases(@PathVariable String slug) {
        return problemService.findVisibleTestCases(slug);
    }
}