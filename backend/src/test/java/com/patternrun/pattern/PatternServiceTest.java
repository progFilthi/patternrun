package com.patternrun.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.pattern.dto.PatternDetailResponse;
import com.patternrun.pattern.dto.PatternSummaryResponse;
import com.patternrun.problem.ProblemRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatternServiceTest {

    @Mock
    private PatternRepository patternRepository;

    @Mock
    private ProblemRepository problemRepository;

    private PatternService patternService;

    @BeforeEach
    void setUp() {
        patternService = new PatternService(patternRepository, problemRepository);
    }

    @Test
    void findAllReturnsPatternsInLearningPathOrderWithProblemCounts() {
        PatternEntity hashing = pattern("hashing", "Hashing", 1);
        PatternEntity slidingWindow = pattern("sliding-window", "Sliding Window", 2);
        when(patternRepository.findAllByOrderByDifficultyOrderAsc()).thenReturn(List.of(hashing, slidingWindow));
        when(problemRepository.countByPrimaryPattern_Id(hashing.getId())).thenReturn(4L);
        when(problemRepository.countByPrimaryPattern_Id(slidingWindow.getId())).thenReturn(2L);

        List<PatternSummaryResponse> patterns = patternService.findAll();

        assertThat(patterns).extracting(PatternSummaryResponse::slug)
                .containsExactly("hashing", "sliding-window");
        assertThat(patterns).extracting(PatternSummaryResponse::problemCount)
                .containsExactly(4L, 2L);
    }

    @Test
    void findBySlugReturnsTeachingContent() {
        PatternEntity entity = pattern("hashing", "Hashing", 1);
        entity.setSummary("Fast lookup instead of repeated search.");
        entity.setSignal("counts, complements, fast lookup");
        entity.setMentalModel("What have I already seen?");
        entity.setRecognitionRules(List.of("complements", "frequency maps"));
        entity.setTemplate(List.of("seen = {}", "for item in input:"));
        entity.setInvariant("The map represents exactly what has been seen.");
        when(patternRepository.findBySlug("hashing")).thenReturn(Optional.of(entity));
        when(problemRepository.countByPrimaryPattern_Id(entity.getId())).thenReturn(4L);

        PatternDetailResponse response = patternService.findBySlug("hashing");

        assertThat(response.name()).isEqualTo("Hashing");
        assertThat(response.signal()).isEqualTo("counts, complements, fast lookup");
        assertThat(response.recognitionRules()).containsExactly("complements", "frequency maps");
        assertThat(response.template()).isNotEmpty();
        assertThat(response.invariant()).contains("map represents");
        assertThat(response.problemCount()).isEqualTo(4L);
    }

    @Test
    void findBySlugFailsWhenPatternIsUnknown() {
        when(patternRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternService.findBySlug("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void requireBySlugReturnsEntityUsedByOtherServicesForValidation() {
        PatternEntity entity = pattern("dp", "Dynamic Programming", 10);
        when(patternRepository.findBySlug("dp")).thenReturn(Optional.of(entity));

        assertThat(patternService.requireBySlug("dp")).isSameAs(entity);
        verify(patternRepository).findBySlug("dp");
    }

    private PatternEntity pattern(String slug, String name, int order) {
        PatternEntity entity = new PatternEntity();
        entity.setId(UUID.randomUUID());
        entity.setSlug(slug);
        entity.setName(name);
        entity.setSignal(name.toLowerCase() + " signal");
        entity.setDifficultyOrder(order);
        entity.setRecognitionRules(List.of());
        entity.setTemplate(List.of());
        return entity;
    }
}