package com.patternrun.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * The session cookie.
 *
 * {@code httpOnly} keeps the token away from JavaScript, so a cross-site scripting bug cannot
 * read it. {@code sameSite=Lax} means the browser does not attach it to cross-site requests,
 * which is what makes the write endpoints safe without a separate CSRF token.
 *
 * That protection depends on the frontend and the API sharing an origin. In production the
 * Next.js rewrite makes them same-origin; in development they are on localhost:3000 and
 * localhost:8080, which are cross-site to a cookie, so the response also sets {@code SameSite=None;
 * Secure} when configured to. See {@code WebConfig} and the deployment notes.
 */
public final class SessionCookie {

    public static final String NAME = "patternrun_session";
    public static final Duration LIFETIME = Duration.ofDays(30);

    private SessionCookie() {
    }

    public static void write(HttpServletResponse response, String token, Instant expiresAt, boolean crossSite) {
        Cookie cookie = new Cookie(NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) LIFETIME.toSeconds());
        if (crossSite) {
            // Two different localhost ports are cross-site to a cookie, so development needs
            // None. Browsers only accept it together with Secure.
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "None");
        } else {
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "Lax");
        }
        response.addCookie(cookie);
    }

    public static void clear(HttpServletResponse response) {
        Cookie cookie = new Cookie(NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    /** The token from the request, or null when the caller has no session. */
    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}