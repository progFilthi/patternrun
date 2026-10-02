package com.patternrun.progress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SpeedrunRecordRepository extends JpaRepository<SpeedrunRecordEntity, UUID> {

    Optional<SpeedrunRecordEntity> findByUserIdAndProblemId(UUID userId, UUID problemId);

    List<SpeedrunRecordEntity> findByUserIdOrderByBestDurationMsAsc(UUID userId);

    /**
     * Row lock for the personal-best comparison. A key cannot express "pay out only when the
     * record improves", so the check and the update have to happen under a lock instead.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SpeedrunRecordEntity r where r.user.id = :userId and r.problem.id = :problemId")
    Optional<SpeedrunRecordEntity> findForUpdate(
            @Param("userId") UUID userId, @Param("problemId") UUID problemId);
}
