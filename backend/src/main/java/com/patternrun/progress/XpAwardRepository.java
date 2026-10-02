package com.patternrun.progress;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XpAwardRepository extends JpaRepository<XpAwardEntity, UUID> {

    boolean existsByUserIdAndAwardKey(UUID userId, String awardKey);

    /**
     * The learner's total. Derived rather than stored, so a retried request or a failed import
     * cannot leave a counter that disagrees with the ledger it came from.
     */
    @Query("select coalesce(sum(a.xp), 0) from XpAwardEntity a where a.user.id = :userId")
    long totalXp(@Param("userId") UUID userId);

    @Query("""
            select coalesce(sum(a.xp), 0) from XpAwardEntity a
            where a.user.id = :userId and a.awardedAt >= :since
            """)
    long xpSince(@Param("userId") UUID userId, @Param("since") Instant since);

    List<XpAwardEntity> findByAttemptIdOrderByAwardedAtAsc(UUID attemptId);
}
