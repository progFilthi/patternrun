package com.patternrun.mistake;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MistakeRepository extends JpaRepository<MistakeEntity, UUID> {

    List<MistakeEntity> findByUserIdAndResolvedFalseAndDueAtLessThanEqualOrderByDueAtAsc(
            UUID userId, Instant now);

    List<MistakeEntity> findByUserIdAndResolvedFalse(UUID userId);

    List<MistakeEntity> findByUserIdAndProblemIdAndResolvedFalse(UUID userId, UUID problemId);

    Optional<MistakeEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndResolvedFalse(UUID userId);

    long countByUserIdAndResolvedTrue(UUID userId);
}
