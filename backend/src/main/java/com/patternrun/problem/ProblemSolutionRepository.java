package com.patternrun.problem;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reference implementations, which exist to be revealed rather than served.
 *
 * Phase 1 created the table and Phase 3 deliberately left it unreferenced, so nothing could leak
 * a solution through an endpoint by accident. Phase 4 adds the one read the product actually
 * needs — the final escape hatch — and no read that returns a solution without recording it
 * against an attempt first.
 */
public interface ProblemSolutionRepository extends JpaRepository<ProblemSolutionEntity, UUID> {

    Optional<ProblemSolutionEntity> findByProblemIdAndLanguage(UUID problemId, ProgrammingLanguage language);
}