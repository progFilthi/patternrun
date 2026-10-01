package com.patternrun.problem;

import com.patternrun.common.PageResponse;
import com.patternrun.problem.dto.AnimationStepResponse;
import com.patternrun.problem.dto.HintResponse;
import com.patternrun.problem.dto.ProblemDetailResponse;
import com.patternrun.problem.dto.ProblemSummaryResponse;
import com.patternrun.problem.dto.TestCaseResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public PageResponse<ProblemSummaryResponse> findAll(
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) Difficulty difficulty,
            @PageableDefault(size = 20, sort = "externalId") Pageable pageable) {
        return PageResponse.from(problemService.findPage(pattern, difficulty, pageable));
    }

    @GetMapping("/{slug}")
    public ProblemDetailResponse findBySlug(@PathVariable String slug) {
        return problemService.findBySlug(slug);
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