package com.patternrun.execution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeExecutionRepository extends JpaRepository<CodeExecutionEntity, UUID> {

    /**
 * The last evaluation, ignoring Runs.
 *
 * This, and not "the last execution", is what decides which hint a learner gets. A Run is a
 * diagnostic: someone who submitted, got a wrong answer, then ran the same code to narrow it down
 * and watched it pass the examples is still wrong, and still needs a debugging hint. Letting a Run
 * reset the context would swap the debugging ladder for the conceptual one exactly when the
 * learner is mid-diagnosis, which reads as the tool forgetting what they had just been told.
 */
Optional<CodeExecutionEntity> findFirstByAttemptIdAndKindOrderByCreatedAtDesc(
        UUID attemptId, ExecutionKind kind);

    /** The most recent execution of any kind. A fallback for before the first submit exists. */
    Optional<CodeExecutionEntity> findFirstByAttemptIdOrderByCreatedAtDesc(UUID attemptId);

    /** Ordered history for one problem, which is what the mistake journal and analytics read. */
    List<CodeExecutionEntity> findByUserIdAndProblemIdOrderByCreatedAtDesc(
            UUID userId, UUID problemId, Pageable pageable);
}