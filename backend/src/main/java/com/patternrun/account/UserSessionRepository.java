package com.patternrun.account;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {

    /**
     * The user is fetched eagerly because the caller needs it after this transaction closes.
     *
     * The filter resolves the session inside a service transaction and hands the result to a
     * controller, so with {@code open-in-view: false} a lazy proxy would be uninitialisable by
     * the time anything read a field from it. Loading it here keeps every later use inside a
     * session that actually exists.
     */
    @EntityGraph(attributePaths = "user")
    Optional<UserSessionEntity> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);

    long deleteByExpiresAtBefore(Instant cutoff);
}
