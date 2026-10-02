package com.patternrun.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.patternrun.account.UserEntity;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies the write rate limit and the response headers required by README section 90.
 *
 * The limit applies to state-changing requests only. Reading content is free and should stay
 * that way, including for a learner on a slow connection.
 */
@Component
@Order(2)
public class WriteGuardFilter extends OncePerRequestFilter {

    private static final int MAX_RETRY_AFTER_SECONDS = 60;
    /** Jakarta Servlet has no SC_ constant for 429. */
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final WriteRateLimiter rateLimiter;

    public WriteGuardFilter(WriteRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        applySecurityHeaders(response);

        if (isWrite(request)) {
            UserEntity currentUser = SessionAuthenticationFilter.currentUser(request);
            // An identified caller is limited per account. Otherwise fall back to the address,
            // which is the most a caller without a session can be distinguished by.
            String caller = currentUser != null
                    ? "user:" + currentUser.getId()
                    : "ip:" + request.getRemoteAddr();
            try {
                rateLimiter.check(caller);
            } catch (RateLimitExceededException ex) {
                response.setHeader("Retry-After", String.valueOf(MAX_RETRY_AFTER_SECONDS));
                response.setStatus(HTTP_TOO_MANY_REQUESTS);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private static boolean isWrite(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    /**
     * Baseline hardening. The API serves JSON only and never reflects markup, so the strictest
     * reasonable policy costs nothing here.
     */
    private static void applySecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cross-Origin-Resource-Policy", "same-site");
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
    }
}