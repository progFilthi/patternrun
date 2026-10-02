package com.patternrun.attempt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptRepository extends JpaRepository<AttemptEntity, UUID> {

    Optional<AttemptEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<AttemptEntity> findByUserIdAndProblemIdAndStatus(UUID userId, UUID problemId, AttemptStatus status);

    List<AttemptEntity> findByUserIdAndStatusOrderByCompletedAtDesc(UUID userId, AttemptStatus status);

    long countByUserIdAndProblemId(UUID userId, UUID problemId);

    List<AttemptEntity> findByUserIdAndProblemIdOrderByCompletedAtAsc(UUID userId, UUID problemId);

    /** Rolling window for the daily rollups and the §88 analytics. */
    @Query("""
            select a from AttemptEntity a
            where a.user.id = :userId and a.status = com.patternrun.attempt.AttemptStatus.COMPLETED
              and a.completedAt >= :since
            order by a.completedAt asc
            """)
    List<AttemptEntity> findCompletedSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("""
            select a from AttemptEntity a
            where a.user.id = :userId and a.status = com.patternrun.attempt.AttemptStatus.COMPLETED
            """)
    List<AttemptEntity> findAllCompleted(@Param("userId") UUID userId);

    /** The history a pattern's mastery row is rebuilt from. */
    @Query("""
            select a from AttemptEntity a
            where a.user.id = :userId
              and a.pattern.id = :patternId
              and a.status = com.patternrun.attempt.AttemptStatus.COMPLETED
            order by a.completedAt asc
            """)
    List<AttemptEntity> findCompletedForPattern(
            @Param("userId") UUID userId, @Param("patternId") UUID patternId);

    /**
     * Correct completions in a window, which is the base of the combo.
     *
     * Counted from the database rather than from a session variable so that a refresh, a second
     * tab, or a retried request cannot inflate it.
     */
    @Query("""
            select count(a) from AttemptEntity a
            where a.user.id = :userId
              and a.status = com.patternrun.attempt.AttemptStatus.COMPLETED
              and a.patternCorrect = true
              and a.completedAt >= :from and a.completedAt < :to
            """)
    long countCorrectCompletionsBetween(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
