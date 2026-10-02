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

    /**
     * Look one up, or fail with the wording every caller should use.
     *
     * Owned by the repository because "whose attempt is this" is one question with one answer, and
     * the coding stage needed it too. Duplicating the lookup would have produced two places to get
     * the 404-versus-403 rule right, and getting it wrong in one of them turns the API into a way
     * to test whether an attempt id exists.
     */
    default AttemptEntity requireOwned(UUID userId, UUID attemptId) {
        return findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new com.patternrun.common.ResourceNotFoundException(
                        "Attempt not found: " + attemptId));
    }

    /**
     * Look one up and require it to still be open.
     *
     * Shared with the coding stage so a finished session fails identically whether the learner is
     * submitting code or finishing the session, instead of one path allowing writes to a completed
     * attempt and the other not.
     */
    default AttemptEntity requireLive(UUID userId, UUID attemptId) {
        AttemptEntity attempt = requireOwned(userId, attemptId);
        if (attempt.isCompleted()) {
            throw new com.patternrun.security.ConflictException(
                    "This session is already finished. Start a new attempt to keep going.");
        }
        return attempt;
    }
}
