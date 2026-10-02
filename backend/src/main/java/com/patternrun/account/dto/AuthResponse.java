package com.patternrun.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * The caller's identity plus the session, if one was issued.
 *
 * {@code sessionToken} is the raw cookie value and is deliberately not serialised. It travels
 * from the service to the controller so a cookie can be written, and stops there: a token in a
 * JSON body would be readable by any script on the page, which is exactly what the httpOnly
 * cookie exists to prevent.
 *
 * A null {@code sessionExpiresAt} means the caller already had a usable session and the cookie
 * should be left exactly as it is.
 */
public record AuthResponse(UserResponse user, Instant sessionExpiresAt, @JsonIgnore String sessionToken) {

    public AuthResponse(UserResponse user, String sessionToken) {
        this(user, sessionToken == null ? null : Instant.now(), sessionToken);
    }

    /** Used when an existing session is reused: nothing to write. */
    public static AuthResponse reused(UserResponse user) {
        return new AuthResponse(user, null, null);
    }
}