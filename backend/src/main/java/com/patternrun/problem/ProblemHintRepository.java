package com.patternrun.problem;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemHintRepository extends JpaRepository<ProblemHintEntity, UUID> {

    List<ProblemHintEntity> findByProblemIdOrderByLevelAsc(UUID problemId);

    long countByProblemId(UUID problemId);
}