package com.patternrun.pattern;

import com.patternrun.pattern.dto.PatternDetailResponse;
import com.patternrun.pattern.dto.PatternRef;
import com.patternrun.pattern.dto.PatternSummaryResponse;
import java.util.List;
public final class PatternMapper {

    private PatternMapper() {
    }

    public static PatternRef toRef(PatternEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PatternRef(entity.getId(), entity.getSlug(), entity.getName());
    }

    public static PatternSummaryResponse toSummary(PatternEntity entity, long problemCount) {
        return new PatternSummaryResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getName(),
                entity.getSignal(),
                entity.getDifficultyOrder(),
                problemCount);
    }

    public static PatternDetailResponse toDetail(PatternEntity entity, long problemCount) {
        return new PatternDetailResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getName(),
                entity.getSummary(),
                entity.getSignal(),
                entity.getMentalModel(),
                List.copyOf(entity.getRecognitionRules()),
                List.copyOf(entity.getTemplate()),
                entity.getInvariant(),
                entity.getDifficultyOrder(),
                problemCount);
    }
}