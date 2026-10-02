package com.patternrun.problem;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemHintRepository extends JpaRepository<ProblemHintEntity, UUID> {

    /**
     * The reasoning-stage ladder: the rungs that apply before the editor.
     *
     * Scoped deliberately. Phase 4 added per-stage and per-trigger rungs, and this endpoint feeds
     * the pre-coding ladder UI, so returning those here would hand the learner a debugging hint
     * before they have written a line and would break the five-rung contract callers rely on.
     */
    List<ProblemHintEntity> findByProblemIdAndStageInAndTriggerOrderByLevelAsc(
            UUID problemId, Collection<HintStage> stages, HintTrigger trigger);

    /** Every rung for a problem, staged and triggered alike. The selector narrows these in service. */
    List<ProblemHintEntity> findByProblemIdOrderByLevelAscStageAscTriggerAsc(UUID problemId);

    long countByProblemIdAndStageInAndTrigger(
            UUID problemId, Collection<HintStage> stages, HintTrigger trigger);
}