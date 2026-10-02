package com.patternrun.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.problem.HintTrigger;
import com.patternrun.pattern.PatternEntity;
import com.patternrun.pattern.PatternService;
import com.patternrun.problem.dto.AnimationStepResponse;
import com.patternrun.problem.dto.HintResponse;
import com.patternrun.problem.dto.ProblemDetailResponse;
import com.patternrun.problem.dto.ProblemSummaryResponse;
import com.patternrun.problem.dto.TestCaseResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ProblemHintRepository hintRepository;

    @Mock
    private AnimationStepRepository animationStepRepository;

    @Mock
    private ProblemTestCaseRepository testCaseRepository;

    @Mock
    private PatternService patternService;

    private ProblemService problemService;

    @BeforeEach
    void setUp() {
        problemService = new ProblemService(problemRepository, hintRepository, animationStepRepository,
                testCaseRepository, patternService);
    }

    @Test
    void findPageFiltersByPatternAndValidatesIt() {
        ProblemEntity problem = problem("two-sum", Difficulty.EASY, TrainingDifficulty.RECOGNITION);
        Page<ProblemEntity> page = new PageImpl<>(List.of(problem), PageRequest.of(0, 20), 1);
        when(problemRepository.findByPrimaryPattern_Slug("hashing", PageRequest.of(0, 20))).thenReturn(page);

        Page<ProblemSummaryResponse> result = problemService.findPage("hashing", null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(ProblemSummaryResponse::slug).containsExactly("two-sum");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(patternService).requireBySlug("hashing");
    }

    @Test
    void findPageFallsBackToDifficultyThenToEverything() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(problemRepository.findAllBy(pageable)).thenReturn(Page.empty());

        problemService.findPage(null, null, pageable);

        verify(problemRepository).findAllBy(pageable);
        verify(problemRepository, never()).findByDifficulty(any(), any());
    }

    @Test
    void findPageFiltersByDifficulty() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(problemRepository.findByDifficulty(Difficulty.HARD, pageable)).thenReturn(Page.empty());

        problemService.findPage(" ", Difficulty.HARD, pageable);

        verify(problemRepository).findByDifficulty(Difficulty.HARD, pageable);
        verify(patternService, never()).requireBySlug(any());
    }

    @Test
    void findBySlugReturnsContentCounts() {
        ProblemEntity problem = problem("two-sum", Difficulty.EASY, TrainingDifficulty.RECOGNITION);
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(hintRepository.countByProblemIdAndStageInAndTrigger(any(), any(), eq(HintTrigger.ANY)))
                .thenReturn(5L);
        when(animationStepRepository.countByProblemId(problem.getId())).thenReturn(8L);
        when(testCaseRepository.countByProblemIdAndIsHiddenFalse(problem.getId())).thenReturn(2L);

        ProblemDetailResponse response = problemService.findBySlug("two-sum");

        assertThat(response.slug()).isEqualTo("two-sum");
        assertThat(response.pattern().slug()).isEqualTo("hashing");
        assertThat(response.complexity().time()).isEqualTo("O(n)");
        assertThat(response.hintCount()).isEqualTo(5);
        assertThat(response.animationStepCount()).isEqualTo(8);
        assertThat(response.visibleTestCaseCount()).isEqualTo(2);
    }

    @Test
    void findBySlugFailsWhenProblemIsUnknown() {
        when(problemRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.findBySlug("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void findHintsReturnsLadderInOrder() {
        ProblemEntity problem = problem("two-sum", Difficulty.EASY, TrainingDifficulty.RECOGNITION);
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(hintRepository.findByProblemIdAndStageInAndTriggerOrderByLevelAsc(
                any(), any(), eq(HintTrigger.ANY)))
                .thenReturn(List.of(hint(1), hint(2)));

        List<HintResponse> hints = problemService.findHints("two-sum");

        assertThat(hints).extracting(HintResponse::level).containsExactly(1, 2);
    }

    @Test
    void findAnimationMapsPayloadAsJson() {
        ProblemEntity problem = problem("two-sum", Difficulty.EASY, TrainingDifficulty.RECOGNITION);
        AnimationStepEntity step = new AnimationStepEntity();
        step.setStepOrder(1);
        step.setStepType(AnimationStepType.ARRAY);
        step.setTitle("Start scanning");
        step.setDescription("Look at the current number.");
        step.setText("Array [2, 7, 11, 15], current index 0.");
        step.setPayload(Map.of("values", List.of(2, 7, 11, 15), "currentIndex", 0));
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(animationStepRepository.findByProblemIdOrderByStepOrderAsc(problem.getId()))
                .thenReturn(List.of(step));

        List<AnimationStepResponse> steps = problemService.findAnimation("two-sum");

        assertThat(steps).hasSize(1);
        AnimationStepResponse response = steps.get(0);
        assertThat(response.type()).isEqualTo(AnimationStepType.ARRAY);
        assertThat(response.payload().get("currentIndex").asInt()).isZero();
        assertThat(response.text()).contains("current index 0");
    }

    @Test
    void findVisibleTestCasesNeverReturnsHiddenCases() {
        ProblemEntity problem = problem("two-sum", Difficulty.EASY, TrainingDifficulty.RECOGNITION);
        ProblemTestCaseEntity visible = testCase(1, "Example 1", false);
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdAndIsHiddenFalseOrderByOrdinalAsc(problem.getId()))
                .thenReturn(List.of(visible));

        List<TestCaseResponse> testCases = problemService.findVisibleTestCases("two-sum");

        assertThat(testCases).extracting(TestCaseResponse::label).containsExactly("Example 1");
    }

    private ProblemEntity problem(String slug, Difficulty difficulty, TrainingDifficulty trainingDifficulty) {
        ProblemEntity entity = new ProblemEntity();
        entity.setId(UUID.randomUUID());
        entity.setSlug(slug);
        entity.setTitle("Two Sum");
        entity.setExternalId(1);
        entity.setDifficulty(difficulty);
        entity.setTrainingDifficulty(trainingDifficulty);
        entity.setStatement("Find two numbers that add up to target.");
        entity.setConstraints(List.of());
        entity.setWhyThisPattern("Fast lookup.");
        entity.setBruteForce("O(n^2).");
        entity.setInvariant("Map holds what has been seen.");
        entity.setPseudocode(List.of("seen = {}", "for x in nums: pass"));
        entity.setCommonMistakes(List.of("Storing before checking"));
        entity.setInterviewExplanation("I use a HashMap.");
        entity.setTimeComplexity("O(n)");
        entity.setSpaceComplexity("O(n)");
        entity.setPrimaryPattern(pattern("hashing", "Hashing"));
        entity.setSecondaryPatterns(List.of());
        entity.setExamples(List.of());
        return entity;
    }

    private PatternEntity pattern(String slug, String name) {
        PatternEntity entity = new PatternEntity();
        entity.setId(UUID.randomUUID());
        entity.setSlug(slug);
        entity.setName(name);
        entity.setSignal(slug);
        entity.setMentalModel(slug);
        entity.setInvariant(slug);
        entity.setSummary(slug);
        entity.setRecognitionRules(List.of());
        entity.setTemplate(List.of());
        return entity;
    }

    private ProblemHintEntity hint(int level) {
        ProblemHintEntity entity = new ProblemHintEntity();
        entity.setLevel(level);
        entity.setContent("hint " + level);
        return entity;
    }

    private ProblemTestCaseEntity testCase(int ordinal, String label, boolean hidden) {
        ProblemTestCaseEntity entity = new ProblemTestCaseEntity();
        entity.setOrdinal(ordinal);
        entity.setLabel(label);
        entity.setInputData("input " + ordinal);
        entity.setExpectedOutput("output " + ordinal);
        entity.setIsHidden(hidden);
        return entity;
    }
}