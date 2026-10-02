package com.patternrun.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patternrun.security.SessionCookie;
import com.patternrun.support.ApiIntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * The breakdown step, end to end.
 *
 * Two properties matter more than the rest. The answer key must never appear in a read, because
 * the browser could simply read it before answering; and the verdict must come from the answers
 * the server stored, because a flag in the completion body would be twenty XP for one boolean.
 */
class BreakdownApiIT extends ApiIntegrationTestBase {

    /** Read-only, to assert on the rows the ledger actually produced. */
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /** The seeded keys and answers for two-sum, read from the content rather than assumed. */
    private static final String CORRECT =
            """
            {"answers":[{"key":"given","chosenIndex":0},
                        {"key":"asked","chosenIndex":1},
                        {"key":"shape","chosenIndex":1}]}
            """;

    private Cookie bootstrap() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/session")).andReturn();
        return result.getResponse().getCookie(SessionCookie.NAME);
    }

    private UUID startTwoSum(Cookie session) throws Exception {
        String body = mockMvc.perform(post("/api/v1/attempts")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemSlug":"two-sum","mode":"STANDARD"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    @Test
    @DisplayName("GET breakdown returns the prompts without the answers")
    void servesPromptsWithoutTheKey() throws Exception {
        mockMvc.perform(get("/api/v1/problems/two-sum/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemSlug").value("two-sum"))
                .andExpect(jsonPath("$.prompts", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.prompts[0].key").value("given"))
                .andExpect(jsonPath("$.prompts[0].prompt").value("What are you handed?"))
                .andExpect(jsonPath("$.prompts[0].options", org.hamcrest.Matchers.hasSize(4)));
    }

    @Test
    @DisplayName("the answer key and explanations are absent from the read")
    void neverServesTheAnswer() throws Exception {
        String body = mockMvc.perform(get("/api/v1/problems/two-sum/breakdown"))
                .andReturn().getResponse().getContentAsString();

        // The whole step would be decorative if the browser could read the key first.
        // Asserted on the field names rather than the word "answer", which occurs legitimately
        // in the option text ("exactly one answer exists").
        assertThat(body).doesNotContain("answerIndex");
        assertThat(body).doesNotContain("explanation");
    }

    @Test
    @DisplayName("every seeded problem now offers a reading step")
    void everyProblemHasAReadingStep() throws Exception {
        // This started life as the opposite assertion, that a problem without a breakdown was a
        // 404. Writing the reading steps closed that gap, so the test now guards it instead:
        // the catalogue has no problem you cannot learn to read.
        mockMvc.perform(get("/api/v1/problems/climbing-stairs/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompts.length()").value(3))
                .andExpect(jsonPath("$.prompts[*].options.length()").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("all-correct answers are marked correct with the explanations")
    void evaluatesCorrectAnswers() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/breakdown")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.answered").value(3))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.prompts[0].correct").value(true))
                .andExpect(jsonPath("$.prompts[0].explanation", containsString("target")));
    }

    @Test
    @DisplayName("one wrong answer fails the whole reading")
    void evaluatesPartlyWrongAnswers() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/breakdown")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"key":"given","chosenIndex":0},
                                            {"key":"asked","chosenIndex":0},
                                            {"key":"shape","chosenIndex":1}]}
                                """))
                .andExpect(status().isOk())
                // Identifying what you are given but not what you must return means the
                // statement has not been read, which is the confusion this step exists to catch.
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.prompts[0].correct").value(true))
                .andExpect(jsonPath("$.prompts[1].correct").value(false))
                .andExpect(jsonPath("$.prompts[1].explanation", containsString("Indices")));
    }

    @Test
    @DisplayName("an unanswered prompt counts as wrong rather than skipped")
    void unansweredIsWrong() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/breakdown")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"key":"given","chosenIndex":0}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.answered").value(1));
    }

    @Test
    @DisplayName("a wrong choice cannot be talked into being right")
    void serverOverridesAClaimedCorrectAnswer() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/breakdown")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        // Every index is wrong. The body asserts nothing about correctness, and
                        // the response says so anyway.
                        .content("""
                                {"answers":[{"key":"given","chosenIndex":3},
                                            {"key":"asked","chosenIndex":3},
                                            {"key":"shape","chosenIndex":3}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));

        // And completion cannot assert the opposite.
        String completion = """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","durationMs":120000}
                """;
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdownCorrect").value(false));

        assertThat(awardCount(session, "PROBLEM_BREAKDOWN"))
                .as("the award follows the stored answers, not anything the browser said")
                .isZero();
    }

    @Test
    @DisplayName("reading the problem correctly pays the breakdown award exactly once")
    void correctReadingPaysOnce() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/breakdown")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORRECT))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"complexityTime":"O(n)","complexitySpace":"O(n)","durationMs":120000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdownCorrect").value(true));

        assertThat(awardCount(session, "PROBLEM_BREAKDOWN")).isEqualTo(1);
    }

    @Test
    @DisplayName("the award cannot be claimed through the completion body")
    void completionIgnoresAClientClaim() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        // The field is no longer part of the contract, and Jackson drops unknown properties, so
        // the claim is ignored rather than rejected. Ignored is the right behaviour and the
        // guarantee is the same either way: it changes nothing.
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"complexityTime":"O(n)","complexitySpace":"O(n)","durationMs":120000,
                                 "breakdownCorrect":true}
                                """))
                .andExpect(status().isOk())
                // The claim is ignored. Asserted through the verdict and the ledger rather than
                // through a total, which would break on any unrelated change to XP values.
                .andExpect(jsonPath("$.breakdownCorrect").value(false));

        assertThat(awardCount(session, "PROBLEM_BREAKDOWN"))
                .as("a flag in the request body must not be worth twenty XP")
                .isZero();
    }

    @Test
    @DisplayName("the breakdown step needs a session")
    void requiresASession() throws Exception {
        mockMvc.perform(post("/api/v1/attempts/some-id/breakdown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORRECT))
                .andExpect(status().isUnauthorized());
    }

    private long awardCount(Cookie session, String reason) throws Exception {
        String userId = mockMvc.perform(get("/api/v1/auth/me").cookie(session))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"user\":\\{\"id\":\"([^\"]+)\".*", "$1");
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from user_xp_awards where user_id = cast(? as uuid) and reason = ?",
                Long.class, userId, reason);
        return count == null ? 0 : count;
    }
}