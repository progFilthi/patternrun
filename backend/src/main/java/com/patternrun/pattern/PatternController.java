package com.patternrun.pattern;

import com.patternrun.pattern.dto.PatternDetailResponse;
import com.patternrun.pattern.dto.PatternSummaryResponse;
import com.patternrun.problem.ProblemService;
import com.patternrun.problem.dto.ProblemSummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patterns")
public class PatternController {

    private final PatternService patternService;
    private final ProblemService problemService;

    public PatternController(PatternService patternService, ProblemService problemService) {
        this.patternService = patternService;
        this.problemService = problemService;
    }

    @GetMapping
    public List<PatternSummaryResponse> findAll() {
        return patternService.findAll();
    }

    @GetMapping("/{slug}")
    public PatternDetailResponse findBySlug(@PathVariable String slug) {
        return patternService.findBySlug(slug);
    }

    @GetMapping("/{slug}/problems")
    public List<ProblemSummaryResponse> findProblems(@PathVariable String slug) {
        return problemService.findByPatternSlug(slug);
    }
}