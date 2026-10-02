package com.patternrun.account;

import com.patternrun.security.SessionTokens;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session lookup.
 *
 * A separate service rather than a method on the filter, because a filter that also carries
 * {@code @Transactional} gets wrapped in a proxy, and a proxied filter is a well-known source of
 * trouble: the servlet container and MockMvc both need the real filter, and initialisation
 * through the proxy does not reach it. The transaction lives here instead, where the proxy is
 * exactly what we want.
 */
@Service
public class SessionService {

    /** A long enough gap that the write is occasional, short enough to stay useful. */
    private static final long LAST_SEEN_REFRESH_SECONDS = 3600;

    private final UserSessionRepository sessions;

    public SessionService(UserSessionRepository sessions) {
        this.sessions = sessions;
    }

    /**
     * The learner behind a token, if there is one.
     *
     * An absent or expired session is not an error. Anonymous learners are the normal case
     * (README section 73), so this only ever answers "who is this, if anyone".
     */
    @Transactional
    public Optional<UserEntity> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<UserSessionEntity> found = sessions.findByTokenHash(SessionTokens.hash(token));
        if (found.isEmpty()) {
            return Optional.empty();
        }

        UserSessionEntity session = found.get();
        Instant now = Instant.now();
        if (session.isExpired(now)) {
            return Optional.empty();
        }

        // Keep the table honest about who is active, without writing on every request.
        if (session.getLastSeenAt().isBefore(now.minusSeconds(LAST_SEEN_REFRESH_SECONDS))) {
            session.setLastSeenAt(now);
            sessions.save(session);
        }
        return Optional.of(session.getUser());
    }

    /** Drops sessions that have aged out, so the table cannot grow without bound. */
    @Transactional
    public long evictExpired(Instant cutoff) {
        return sessions.deleteByExpiresAtBefore(cutoff);
    }
}