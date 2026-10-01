package com.patternrun.problem;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemTestCaseRepository extends JpaRepository<ProblemTestCaseEntity, UUID> {

    List<ProblemTestCaseEntity> findByProblemIdAndIsHiddenFalseOrderByOrdinalAsc(UUID problemId);

    long countByProblemIdAndIsHiddenFalse(UUID problemId);
}