package com.patternrun.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Session token generation and hashing.
 *
 * The cookie carries a 256-bit random value and the database stores only its SHA-256 hash. A
 * database read, backup or SQL console therefore cannot produce a usable session cookie: the
 * hash does not let anyone reconstruct the token.
 *
 * The tokens are random rather than signed, which means revocation is a single delete. A
 * stateless signed cookie would need a denylist to log anyone out, reintroducing the state this
 * design avoids.
 */
public final class SessionTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private SessionTokens() {
    }

    /** A fresh high-entropy token, safe to place in a cookie. */
    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The value to persist. Deterministic, so lookup is a plain indexed equality match. */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is required of every JVM; its absence is unrecoverable.
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}