package com.patternrun.mistake;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MistakeRepository extends JpaRepository<MistakeEntity, UUID> {

    List<MistakeEntity> findByUserIdAndResolvedFalseAndDueAtLessThanEqualOrderByDueAtAsc(
            UUID userId, Instant now);

    /**
     * The open entry for one problem and category, if there is one.
     *
     * Used to avoid piling up duplicates. A learner who has failed the same thing three times has
     * one thing to review, not three, and a queue that grows on every retry trains the learner to
     * ignore it.
     */
    Optional<MistakeEntity> findByUserIdAndProblemIdAndCategoryAndResolvedFalse(
            UUID userId, UUID problemId, MistakeCategory category);

    List<MistakeEntity> findByUserIdAndProblemIdAndResolvedFalse(UUID userId, UUID problemId);

    Optional<MistakeEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndResolvedFalse(UUID userId);

    long countByUserIdAndResolvedTrue(UUID userId);
}
