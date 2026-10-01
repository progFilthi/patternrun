package com.patternrun.pattern;

import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.pattern.dto.PatternDetailResponse;
import com.patternrun.pattern.dto.PatternSummaryResponse;
import com.patternrun.problem.ProblemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PatternService {

    private final PatternRepository patternRepository;
    private final ProblemRepository problemRepository;

    public PatternService(PatternRepository patternRepository, ProblemRepository problemRepository) {
        this.patternRepository = patternRepository;
        this.problemRepository = problemRepository;
    }

    public List<PatternSummaryResponse> findAll() {
        return patternRepository.findAllByOrderByDifficultyOrderAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    public PatternDetailResponse findBySlug(String slug) {
        PatternEntity pattern = requireBySlug(slug);
        return PatternMapper.toDetail(pattern, problemRepository.countByPrimaryPattern_Id(pattern.getId()));
    }

    public PatternEntity requireBySlug(String slug) {
        return patternRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern not found: " + slug));
    }

    private PatternSummaryResponse toSummary(PatternEntity pattern) {
        return PatternMapper.toSummary(pattern, problemRepository.countByPrimaryPattern_Id(pattern.getId()));
    }
}