package com.patternrun.problem;

import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.pattern.PatternService;
import com.patternrun.problem.dto.AnimationStepResponse;
import com.patternrun.problem.dto.BreakdownResponse;
import com.patternrun.problem.dto.HintResponse;
import com.patternrun.problem.dto.ProblemDetailResponse;
import com.patternrun.problem.dto.ProblemSummaryResponse;
import com.patternrun.problem.dto.TestCaseResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemHintRepository hintRepository;
    private final AnimationStepRepository animationStepRepository;
    private final ProblemTestCaseRepository testCaseRepository;
    private final PatternService patternService;

    public ProblemService(ProblemRepository problemRepository,
                          ProblemHintRepository hintRepository,
                          AnimationStepRepository animationStepRepository,
                          ProblemTestCaseRepository testCaseRepository,
                          PatternService patternService) {
        this.problemRepository = problemRepository;
        this.hintRepository = hintRepository;
        this.animationStepRepository = animationStepRepository;
        this.testCaseRepository = testCaseRepository;
        this.patternService = patternService;
    }

    public Page<ProblemSummaryResponse> findPage(String patternSlug, Difficulty difficulty, Pageable pageable) {
        Page<ProblemEntity> page;
        if (patternSlug != null && !patternSlug.isBlank()) {
            patternService.requireBySlug(patternSlug);
            page = problemRepository.findByPrimaryPattern_Slug(patternSlug, pageable);
        } else if (difficulty != null) {
            page = problemRepository.findByDifficulty(difficulty, pageable);
        } else {
            page = problemRepository.findAllBy(pageable);
        }
        return page.map(ProblemMapper::toSummary);
    }

    public List<ProblemSummaryResponse> findByPatternSlug(String patternSlug) {
        patternService.requireBySlug(patternSlug);
        return problemRepository.findByPrimaryPattern_SlugOrderByExternalIdAsc(patternSlug).stream()
                .map(ProblemMapper::toSummary)
                .toList();
    }

    public ProblemDetailResponse findBySlug(String slug) {
        ProblemEntity problem = requireBySlug(slug);
        return ProblemMapper.toDetail(
                problem,
                hintRepository.countByProblemId(problem.getId()),
                animationStepRepository.countByProblemId(problem.getId()),
                testCaseRepository.countByProblemIdAndIsHiddenFalse(problem.getId()));
    }

    /**
     * The prompts that break the statement down into what is given, what is asked and which
     * constraint matters.
     *
     * Answer indices and explanations are projected away. They come back from the attempt
     * endpoint once the learner has committed, so serving them here would let the browser read
     * the key out of the response before trying.
     */
    public BreakdownResponse findBreakdown(String slug) {
        List<BreakdownPrompt> prompts = problemRepository.findBySlug(slug)
                .map(ProblemEntity::getBreakdown)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + slug));
        if (prompts.isEmpty()) {
            throw new ResourceNotFoundException("No breakdown for problem: " + slug);
        }
        return BreakdownResponse.of(slug, prompts);
    }

    public List<HintResponse> findHints(String slug) {
        return hintRepository.findByProblemIdOrderByLevelAsc(requireBySlug(slug).getId()).stream()
                .map(ProblemMapper::toHint)
                .toList();
    }

    public List<AnimationStepResponse> findAnimation(String slug) {
        return animationStepRepository.findByProblemIdOrderByStepOrderAsc(requireBySlug(slug).getId()).stream()
                .map(ProblemMapper::toAnimationStep)
                .toList();
    }

    /** Hidden test cases are never exposed (README section 47). */
    public List<TestCaseResponse> findVisibleTestCases(String slug) {
        return testCaseRepository.findByProblemIdAndIsHiddenFalseOrderByOrdinalAsc(requireBySlug(slug).getId()).stream()
                .map(ProblemMapper::toTestCase)
                .toList();
    }

    private ProblemEntity requireBySlug(String slug) {
        return problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + slug));
    }
}