package com.patternrun.account;

import com.patternrun.account.dto.AuthResponse;
import com.patternrun.account.dto.LoginRequest;
import com.patternrun.account.dto.RegisterRequest;
import com.patternrun.security.CurrentUser;
import com.patternrun.security.SessionCookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accounts (README section 73).
 *
 * {@code POST /session} is the one call a browser makes on load. It returns the caller's
 * identity and, if they had none, creates an anonymous row and sets a cookie. There is no
 * "sign up to start" wall: an anonymous learner is a first-class learner.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    /** Establishes a session. Idempotent: an existing session is returned untouched. */
    @PostMapping("/session")
    public AuthResponse session(
            @CurrentUser(required = false) UserEntity current,
            @RequestHeader(value = "X-Timezone", required = false) String timezone,
            HttpServletResponse response) {
        AuthResponse auth = accounts.bootstrap(current, timezone);
        accounts.writeSessionCookie(response, auth);
        return auth;
    }

    @GetMapping("/me")
    public AuthResponse me(@CurrentUser UserEntity current, HttpServletResponse response) {
        AuthResponse auth = accounts.describe(current);
        accounts.writeSessionCookie(response, auth);
        return auth;
    }

    @PostMapping("/register")
    public AuthResponse register(
            @CurrentUser(required = false) UserEntity current,
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse auth = accounts.register(current, request);
        accounts.writeSessionCookie(response, auth);
        return auth;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse auth = accounts.login(request);
        accounts.writeSessionCookie(response, auth);
        return auth;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CurrentUser(required = false) UserEntity current, HttpServletResponse response) {
        accounts.logout(current);
        SessionCookie.clear(response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}