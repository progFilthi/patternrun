package com.patternrun.security;

import com.patternrun.account.SessionService;
import com.patternrun.account.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the caller's identity from the session cookie.
 *
 * Deliberately thin. It reads the cookie, asks {@link SessionService} who that is, and puts the
 * answer on the request. Everything with a database or a transaction behind it lives in the
 * service, because a filter that carried its own {@code @Transactional} would be wrapped in a
 * proxy and a proxied filter cannot be initialised properly by the container or by MockMvc.
 *
 * A missing or expired session is not an error here. Anonymous learners are the normal case
 * (README section 73), so this only ever answers "who is this, if anyone". Endpoints that need
 * a learner say so with {@link CurrentUser}, and it is the argument resolver that decides
 * whether absence is acceptable.
 */
@Component
@Order(1)
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String PRINCIPAL_ATTRIBUTE = "patternrun.user";

    private final SessionService sessions;

    public SessionAuthenticationFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        sessions.resolve(SessionCookie.read(request))
                .ifPresent(user -> request.setAttribute(PRINCIPAL_ATTRIBUTE, user));
        chain.doFilter(request, response);
    }

    /** The caller, when there is one. Never throws: absence is a legitimate state. */
    public static UserEntity currentUser(HttpServletRequest request) {
        Object principal = request.getAttribute(PRINCIPAL_ATTRIBUTE);
        return principal instanceof UserEntity user ? user : null;
    }
}