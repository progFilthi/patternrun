package com.patternrun.account;

import com.patternrun.account.dto.AuthResponse;
import com.patternrun.account.dto.LoginRequest;
import com.patternrun.account.dto.RegisterRequest;
import com.patternrun.config.CorsProperties;
import com.patternrun.security.ConflictException;
import com.patternrun.security.SessionCookie;
import com.patternrun.security.SessionTokens;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Identity.
 *
 * The decision that matters is in {@link #bootstrap}: every visitor gets an anonymous row on
 * their first session, so progress is server-side from the very first session rather than
 * starting when they register. Registering then claims that same row instead of creating a new
 * one, which is why nothing has to be migrated when an account appears (README section 73).
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final CorsProperties corsProperties;

    public AccountService(
            UserRepository users,
            UserSessionRepository sessions,
            PasswordEncoder passwordEncoder,
            CorsProperties corsProperties) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.corsProperties = corsProperties;
    }

    /**
     * The caller's identity, creating an anonymous one if they have no session.
     *
     * An existing session is left completely alone. Rotating it here would mean every page
     * load replaced the cookie, and two tabs open at once would fight over it, with one
     * silently invalidating the other.
     */
    @Transactional
    public AuthResponse bootstrap(UserEntity current, String requestedTimezone) {
        if (current != null) {
            return AuthResponse.reused(AccountMapper.toResponse(current));
        }
        UserEntity anonymous = new UserEntity();
        anonymous.setAnonymous(true);
        anonymous.setTimezone(normaliseTimezone(requestedTimezone));
        users.save(anonymous);
        log.debug("Created anonymous learner {}", anonymous.getId());
        String token = issueSession(anonymous);
        return new AuthResponse(AccountMapper.toResponse(anonymous), token);
    }

    /**
     * Claims the current anonymous row for a real account, or registers a fresh one.
     *
     * Claiming is the whole point: the row keeps its id, so attempts, mastery and streaks stay
     * exactly where they already are.
     */
    @Transactional
    public AuthResponse register(UserEntity current, RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("That email is already registered. Sign in instead.");
        }

        UserEntity user = current != null ? current : new UserEntity();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUsername(blankToNull(request.username()));
        user.setAnonymous(false);
        user.setClaimedAt(Instant.now());
        user.setTimezone(normaliseTimezone(request.timezone()));
        users.save(user);

        // A registration always rotates the session, so a token that was somehow in the wild
        // before the account existed cannot become an account token.
        log.info("Learner {} registered", user.getId());
        return new AuthResponse(AccountMapper.toResponse(user), issueSession(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = users
                .findByEmailIgnoreCase(request.email().trim())
                .filter(candidate -> candidate.getPasswordHash() != null)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ConflictException("That email and password do not match."));

        return new AuthResponse(AccountMapper.toResponse(user), issueSession(user));
    }

    @Transactional
    public void logout(UserEntity current) {
        if (current != null) {
            sessions.deleteByUserId(current.getId());
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse describe(UserEntity current) {
        return AuthResponse.reused(AccountMapper.toResponse(current));
    }

    private String issueSession(UserEntity user) {
        String token = SessionTokens.newToken();
        UserSessionEntity session = new UserSessionEntity();
        session.setUser(user);
        session.setTokenHash(SessionTokens.hash(token));
        session.setExpiresAt(Instant.now().plus(SessionCookie.LIFETIME));
        session.setLastSeenAt(Instant.now());
        sessions.save(session);
        return token;
    }

    /** Writes the cookie when a new session was issued, and leaves it alone otherwise. */
    public void writeSessionCookie(HttpServletResponse response, AuthResponse auth) {
        if (auth.sessionExpiresAt() != null) {
            SessionCookie.write(
                    response,
                    auth.sessionToken(),
                    auth.sessionExpiresAt(),
                    !corsProperties.isSameOriginFrontend());
        }
    }

    /**
     * A browser-reported zone is untrusted input. An unrecognised zone falls back to UTC rather
     * than failing the request: a bad zone should not cost someone their session, and UTC is a
     * safe default for day bucketing.
     */
    static String normaliseTimezone(String requested) {
        if (requested == null || requested.isBlank()) {
            return "UTC";
        }
        try {
            return ZoneId.of(requested.trim()).getId();
        } catch (RuntimeException ex) {
            return "UTC";
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}