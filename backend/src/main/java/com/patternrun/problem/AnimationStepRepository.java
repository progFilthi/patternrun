package com.patternrun.problem;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimationStepRepository extends JpaRepository<AnimationStepEntity, UUID> {

    List<AnimationStepEntity> findByProblemIdOrderByStepOrderAsc(UUID problemId);

    long countByProblemId(UUID problemId);
}