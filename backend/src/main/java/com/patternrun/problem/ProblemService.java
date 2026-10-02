package com.patternrun.problem;

import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.pattern.PatternService;
import com.patternrun.problem.dto.AnimationStepResponse;
import com.patternrun.problem.dto.BreakdownResponse;
import com.patternrun.problem.dto.HintResponse;
import com.patternrun.problem.dto.HintSelection;
import com.patternrun.problem.dto.ProblemDetailResponse;
import com.patternrun.problem.dto.ProblemSummaryResponse;
import com.patternrun.problem.dto.TestCaseResponse;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProblemService {

    /**
     * Stages whose rungs belong to the pre-coding ladder.
     *
     * {@code ANY} is every rung authored before staging existed, and {@code REASONING} is the
     * deliberately generic reasoning ladder. {@code CODING} is absent on purpose: its rungs
     * assume there is code on screen, and some assume it has just failed.
     */
    private static final List<HintStage> REASONING_LADDER_STAGES =
            List.of(HintStage.ANY, HintStage.REASONING);

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
                reasoningHintCount(problem.getId()),
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

    /**
     * The pre-coding hint ladder.
     *
     * {@code REASONING_LADDER_STAGES} is the set of stages whose rungs are shown before the
     * editor. {@code CODING} is excluded on purpose: those rungs assume the learner has code on
     * screen, and one of them assumes they have just seen it fail.
     */
    public List<HintResponse> findHints(String slug) {
        return hintRepository
                .findByProblemIdAndStageInAndTriggerOrderByLevelAsc(
                        requireBySlug(slug).getId(), REASONING_LADDER_STAGES, HintTrigger.ANY)
                .stream()
                .map(ProblemMapper::toHint)
                .toList();
    }

    /** The count behind {@code hintCount}, scoped the same way as {@link #findHints}. */
    private long reasoningHintCount(UUID problemId) {
        return hintRepository.countByProblemIdAndStageInAndTrigger(
                problemId, REASONING_LADDER_STAGES, HintTrigger.ANY);
    }

    /**
     * The next rung for the moment the learner is actually in.
     *
     * Selection, not filtering: rungs specific to the requested stage and trigger are offered
     * first, and the generic ladder is the fallback when nothing more precise exists. That
     * ordering is what stops the five-rung general ladder from shadowing every purpose-built hint.
     *
     * Returns at most one rung, so the caller reveals it rather than handing over a list. A hint
     * system that shows three at once is an explanation screen wearing a hint's clothes.
     */
    public HintSelection findHintFor(UUID problemId, HintStage stage, HintTrigger trigger) {
        List<ProblemHintEntity> rungs =
                hintRepository.findByProblemIdOrderByLevelAscStageAscTriggerAsc(problemId);

        // Specificity first, level second. Sorting by level alone does not work: a repository
        // ordering by the stored strings puts ANY before CODING and before WRONG_ANSWER
        // alphabetically, so the generic rung shadowed every purpose-built hint and the staging was
        // decorative.
        //
        // Specificity counts both dimensions. A CODING rung with a generic trigger is still less
        // specific than the same stage with a matching one, and ranking on the stage alone would
        // let it win the tie at the same level. Ordering here rather than in the query because the
        // rule is a policy about which hint is more helpful, and policies belong in code where they
        // can be commented.
        return rungs.stream()
                .filter(rung -> rung.isSpecificTo(stage, trigger))
                .min(Comparator
                        .comparingInt((ProblemHintEntity rung) -> rung.getStage() == HintStage.ANY ? 1 : 0)
                        .thenComparingInt(rung -> rung.getTrigger() == HintTrigger.ANY ? 1 : 0)
                        .thenComparingInt(ProblemHintEntity::getLevel))
                .map(ProblemMapper::toHintSelection)
                .orElse(null);
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