package com.patternrun.problem;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.patternrun.pattern.PatternEntity;
import com.patternrun.pattern.PatternMapper;
import com.patternrun.pattern.dto.PatternRef;
import com.patternrun.problem.dto.AnimationStepResponse;
import com.patternrun.problem.dto.ComplexityResponse;
import com.patternrun.problem.dto.ExampleResponse;
import com.patternrun.problem.dto.HintResponse;
import com.patternrun.problem.dto.ProblemDetailResponse;
import com.patternrun.problem.dto.ProblemSummaryResponse;
import com.patternrun.problem.dto.TestCaseResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ProblemMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ProblemMapper() {
    }

    public static ProblemSummaryResponse toSummary(ProblemEntity entity) {
        return new ProblemSummaryResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getExternalId(),
                entity.getDifficulty(),
                entity.getTrainingDifficulty(),
                PatternMapper.toRef(entity.getPrimaryPattern()),
                complexity(entity));
    }

    public static ProblemDetailResponse toDetail(ProblemEntity entity,
                                                 long hintCount,
                                                 long animationStepCount,
                                                 long visibleTestCaseCount) {
        List<ExampleResponse> examples = entity.getExamples().stream()
                .sorted(Comparator.comparing(ProblemExampleEntity::getOrdinal))
                .map(example -> new ExampleResponse(
                        example.getOrdinal(),
                        example.getInput(),
                        example.getOutput(),
                        example.getExplanation()))
                .toList();

        return new ProblemDetailResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getExternalId(),
                entity.getDifficulty(),
                entity.getTrainingDifficulty(),
                entity.getStatement(),
                List.copyOf(entity.getConstraints()),
                examples,
                PatternMapper.toRef(entity.getPrimaryPattern()),
                secondaryPatterns(entity),
                List.copyOf(entity.getPseudocode()),
                complexity(entity),
                entity.getWhyThisPattern(),
                entity.getBruteForce(),
                entity.getInvariant(),
                entity.getInterviewExplanation(),
                List.copyOf(entity.getCommonMistakes()),
                hintCount,
                animationStepCount,
                visibleTestCaseCount);
    }

    public static HintResponse toHint(ProblemHintEntity entity) {
        return new HintResponse(entity.getLevel(), entity.getContent());
    }

    public static AnimationStepResponse toAnimationStep(AnimationStepEntity entity) {
        return new AnimationStepResponse(
                entity.getStepOrder(),
                entity.getStepType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getText(),
                toJson(entity.getPayload()));
    }

    public static TestCaseResponse toTestCase(ProblemTestCaseEntity entity) {
        return new TestCaseResponse(
                entity.getOrdinal(),
                entity.getLabel(),
                entity.getInputData(),
                entity.getExpectedOutput());
    }

    private static ComplexityResponse complexity(ProblemEntity entity) {
        return new ComplexityResponse(entity.getTimeComplexity(), entity.getSpaceComplexity());
    }

    private static List<PatternRef> secondaryPatterns(ProblemEntity entity) {
        return entity.getSecondaryPatterns().stream()
                .sorted(Comparator.comparing(PatternEntity::getName))
                .map(PatternMapper::toRef)
                .toList();
    }

    private static JsonNode toJson(Map<String, Object> payload) {
        return OBJECT_MAPPER.valueToTree(payload);
    }
}