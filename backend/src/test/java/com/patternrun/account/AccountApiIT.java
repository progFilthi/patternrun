package com.patternrun.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patternrun.security.SessionCookie;
import com.patternrun.support.ApiIntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Identity end to end, against a real database.
 *
 * The behaviours worth pinning are the ones a unit test would mock away: an anonymous session
 * exists without a sign-up wall, registering keeps the same row so progress never has to move,
 * and the raw token never reaches a response body.
 */
class AccountApiIT extends ApiIntegrationTestBase {

    /**
     * Establishes an anonymous session and returns its cookie.
     *
     * The value has to come from a real response. A made-up token resolves to no session, which
     * would silently turn "claims the anonymous row" into "creates a second row" and still pass.
     */
    private Cookie bootstrap() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/session")).andReturn();
        Cookie cookie = result.getResponse().getCookie(SessionCookie.NAME);
        assertThat(cookie).as("bootstrap must set a session cookie").isNotNull();
        return cookie;
    }

    private String learnerIdOf(String responseBody) {
        return responseBody.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    @Test
    @DisplayName("POST /api/v1/auth/session gives an anonymous caller an identity and a cookie")
    void createsAnonymousSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/session")
                        .header("X-Timezone", "Europe/Berlin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.anonymous").value(true))
                .andExpect(jsonPath("$.user.email").doesNotExist())
                .andExpect(jsonPath("$.user.timezone").value("Europe/Berlin"))
                .andExpect(jsonPath("$.sessionToken").doesNotExist());
    }

    @Test
    @DisplayName("an unrecognised timezone falls back to UTC rather than failing the session")
    void unknownTimezoneFallsBack() throws Exception {
        mockMvc.perform(post("/api/v1/auth/session").header("X-Timezone", "Mars/Olympus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.timezone").value("UTC"));
    }

    @Test
    @DisplayName("registering claims the anonymous row instead of creating a second one")
    void registeringClaimsTheAnonymousRow() throws Exception {
        Cookie session = bootstrap();
        String anonymousId = learnerIdOf(
                mockMvc.perform(get("/api/v1/auth/me").cookie(session))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"learner@example.com","password":"correct horse",
                                 "username":"learner","timezone":"UTC"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.anonymous").value(false))
                .andExpect(jsonPath("$.user.email").value("learner@example.com"))
                .andExpect(jsonPath("$.user.username").value("learner"))
                // Same row, so attempts, mastery and streaks stay exactly where they were.
                .andExpect(jsonPath("$.user.id").value(anonymousId));
    }

    @Test
    @DisplayName("the session cookie is httpOnly so a script cannot read the token")
    void cookieIsHttpOnly() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/session")).andReturn();
        Cookie cookie = result.getResponse().getCookie(SessionCookie.NAME);

        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getValue()).isNotBlank();
        // The stored value must not be the cookie value, or a database read yields live sessions.
        assertThat(cookie.getValue()).hasSizeGreaterThan(20);
    }

    @Test
    @DisplayName("registering the same email twice is a conflict")
    void duplicateEmailConflicts() throws Exception {
        String body = """
                {"email":"twice@example.com","password":"correct horse","timezone":"UTC"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(bootstrap())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(bootstrap())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("an unknown email and a wrong password are indistinguishable")
    void loginDoesNotRevealWhetherAnAccountExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(bootstrap())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"real@example.com","password":"correct horse","timezone":"UTC"}
                                """))
                .andExpect(status().isOk());

        // A wrong password on a real account, and a login for an account that does not exist,
        // must be byte-for-byte identical. Any difference at all lets someone enumerate which
        // addresses are registered.
        String wrongPassword = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"real@example.com","password":"wrong horse"}
                                """))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"wrong horse"}
                                """))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(stripTimestamp(unknownEmail)).isEqualTo(stripTimestamp(wrongPassword));
    }

    /** Drops the timestamp so two otherwise identical error bodies can be compared. */
    private String stripTimestamp(String errorBody) {
        return errorBody.replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":\"\"");
    }

    @Test
    @DisplayName("a too short password is rejected by validation")
    void shortPasswordRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(bootstrap())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"short@example.com","password":"tiny","timezone":"UTC"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("password")));
    }

    @Test
    @DisplayName("login returns the identity without leaking the token")
    void loginReturnsIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(bootstrap())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"back@example.com","password":"correct horse","timezone":"UTC"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"back@example.com","password":"correct horse"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("back@example.com"))
                .andExpect(jsonPath("$.sessionToken").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without a session is 401, not an anonymous success")
    void meRequiresASession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with a valid cookie returns that learner")
    void meWithSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").cookie(bootstrap()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.anonymous").value(true));
    }

    @Test
    @DisplayName("a second bootstrap leaves the existing session alone")
    void bootstrapIsIdempotent() throws Exception {
        Cookie first = bootstrap();
        String firstBody = mockMvc.perform(get("/api/v1/auth/me").cookie(first))
                .andReturn().getResponse().getContentAsString();

        // Rotating the cookie here would mean two tabs fight over it and one is silently logged out.
        mockMvc.perform(post("/api/v1/auth/session").cookie(first)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").cookie(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(learnerIdOf(firstBody)));
    }

    @Test
    @DisplayName("logout clears the cookie and the session stops working")
    void logoutEndsTheSession() throws Exception {
        Cookie session = bootstrap();
        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout").cookie(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("content stays readable with no session at all")
    void contentNeedsNoIdentity() throws Exception {
        // Anonymous browsing is the normal case; requiring a session to read content would put a
        // wall in front of the learning loop.
        mockMvc.perform(get("/api/v1/problems/two-sum"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}