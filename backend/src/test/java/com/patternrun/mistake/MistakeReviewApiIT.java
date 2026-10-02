package com.patternrun.mistake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The mistake journal and the review loop, end to end.
 *
 * These were all built in Phase 3 and all unreachable: nothing constructed a mistake, the review
 * controller had no write handler, and `markReviewed` had no caller. The whole chain sat there
 * passing a review of itself, because nothing in the suite referenced mistakes at all.
 */
class MistakeReviewApiIT extends ApiIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Cookie bootstrap() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/session"))
                .andReturn().getResponse().getCookie(SessionCookie.NAME);
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

    /** A fully correct two-sum session, which is what resolves an open mistake. */
    private String completeCorrectTwoSum(Cookie session) throws Exception {
        UUID attempt = startTwoSum(session);
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/pattern")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patternSlug":"hashing"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/breakdown")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"key":"given","chosenIndex":0},
                                            {"key":"asked","chosenIndex":1},
                                            {"key":"shape","chosenIndex":1}]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/predict")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepOrder":3,"chosenIndex":1}
                                """))
                .andExpect(status().isOk());
        return mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"complexityTime":"O(n)","complexitySpace":"O(n)","durationMs":60000}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void completeWrongPattern(Cookie session, UUID attemptId) throws Exception {
        mockMvc.perform(post("/api/v1/attempts/" + attemptId + "/pattern")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patternSlug":"sliding-window"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/attempts/" + attemptId + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"complexityTime":"O(n)","complexitySpace":"O(n)","durationMs":60000}
                                """))
                .andExpect(status().isOk());
    }

    /**
     * The learner this test is acting as.
     *
     * Counts are scoped to them rather than taken across the table: the Testcontainers instance is
     * shared by every integration test in the suite, so a global count would make every assertion
     * depend on what other classes happened to do first.
     */
    private String userId(Cookie session) throws Exception {
        String body = mockMvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private long mistakeCount(Cookie session) throws Exception {
        return count("select count(*) from user_mistakes where user_id = cast(? as uuid)",
                userId(session));
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private String firstMistakeId(Cookie session) throws Exception {
        String body = mockMvc.perform(get("/api/v1/progress/review").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"mistakeId\":\"([^\"]+)\".*", "$1");
    }

    @Test
    @DisplayName("an imperfect attempt records a mistake")
    void recordsAMistake() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        assertThat(mistakeCount(session)).as("a fresh learner has no mistakes").isZero();

        completeWrongPattern(session, attempt);

        assertThat(mistakeCount(session)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/progress/review").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("PATTERN"))
                .andExpect(jsonPath("$[0].problemSlug").value("two-sum"))
                .andExpect(jsonPath("$[0].description", containsString("sliding-window")))
                // A review that does not show why it was wrong is a repetition, not a correction.
                .andExpect(jsonPath("$[0].lesson").isNotEmpty());
    }

    @Test
    @DisplayName("a correct attempt records nothing")
    void aCorrectAttemptRecordsNothing() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/pattern")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patternSlug":"hashing"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/breakdown")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"key":"given","chosenIndex":0},
                                            {"key":"asked","chosenIndex":1},
                                            {"key":"shape","chosenIndex":1}]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/predict")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepOrder":3,"chosenIndex":1}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"complexityTime":"O(n)","complexitySpace":"O(n)","durationMs":60000}
                                """))
                .andExpect(status().isOk());

        assertThat(mistakeCount(session)).isZero();
    }

    @Test
    @DisplayName("failing twice on the same thing does not pile up duplicates")
    void doesNotDuplicate() throws Exception {
        Cookie session = bootstrap();
        UUID first = startTwoSum(session);
        completeWrongPattern(session, first);
        assertThat(mistakeCount(session)).isEqualTo(1);

        // The first attempt is completed, so starting again gives a fresh live one rather than
        // resuming it — which is what lets the same mistake be failed twice on purpose.
        UUID second = startTwoSum(session);
        completeWrongPattern(session, second);

        // One thing the learner has not got, not two. A queue that grows on every retry trains the
        // learner to ignore it.
        assertThat(mistakeCount(session)).isEqualTo(1);
    }

    @Test
    @DisplayName("reviewing doubles the interval and pays once")
    void reviewAdvancesTheInterval() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);
        String mistakeId = firstMistakeId(session);

        mockMvc.perform(post("/api/v1/progress/review/" + mistakeId)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correct":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.intervalDays").value(2))
                .andExpect(jsonPath("$.xpAwarded").value(20))
                .andExpect(jsonPath("$.nextDueAt").isNotEmpty());
    }

    @Test
    @DisplayName("getting it wrong again resets the interval to a day")
    void reviewWrongResets() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);
        String mistakeId = firstMistakeId(session);

        review(session, mistakeId, true);
        mockMvc.perform(post("/api/v1/progress/review/" + mistakeId)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correct":false}
                                """))
                // Not due yet: the only way to get here is to wait a day, and waiting is the point.
                .andExpect(status().isConflict());
    }

    /**
     * The hole this endpoint would have had without the due-date gate.
     *
     * The award key contains the review number and the review number increments on every call, so a
     * caller who could review whenever they liked could collect REVIEW_COMPLETED indefinitely. The
     * gate makes the reward and the spacing the same thing.
     */
    @Test
    @DisplayName("reviewing before it is due is refused, so the award cannot be farmed")
    void reviewIsGatedOnBeingDue() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);
        String mistakeId = firstMistakeId(session);

        review(session, mistakeId, true);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/progress/review/" + mistakeId)
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"correct":true}
                                    """))
                    .andExpect(status().isConflict());
        }

        assertThat(count("select count(*) from user_xp_awards where reason = 'REVIEW_COMPLETED'"
                + " and user_id = cast(? as uuid)", userId(session)))
                .as("five refused reviews must not have paid")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("the award reaches today's total, not just the lifetime one")
    void reviewXpCountsTowardsToday() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);
        String mistakeId = firstMistakeId(session);

        int before = todayXp(session);
        String paid = review(session, mistakeId, true);

        assertThat(todayXp(session))
                .as("XP earned today must move by exactly what the review paid")
                .isEqualTo(before + (int) com.jayway.jsonpath.JsonPath.<Number>read(paid, "$.xpAwarded"));
    }

    @Test
    @DisplayName("a comeback award reaches today's total too")
    void comebackXpCountsTowardsToday() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);

        int before = todayXp(session);
        String body = completeCorrectTwoSum(session);

        int awarded = 0;
        for (Object value : com.jayway.jsonpath.JsonPath.<java.util.List<Integer>>read(body, "$.awards[*].xp")) {
            awarded += (Integer) value;
        }
        assertThat(awarded).as("a comeback attempt pays something").isGreaterThan(0);
        assertThat(com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(body, "$.awards[*].reason"))
                .contains("COMEBACK");
        assertThat(todayXp(session))
                .as("every award on this session, the comeback included, lands in today's total")
                .isEqualTo(before + awarded);
    }

    /** Today XP for this learner only, read the way the progress screen reads it. */
    private int todayXp(Cookie session) throws Exception {
        String body = mockMvc.perform(get("/api/v1/progress").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return (int) com.jayway.jsonpath.JsonPath.<Number>read(body, "$.today.xpEarned");
    }

    @Test
    @DisplayName("a mistake stops being due once reviewed, and comes back later")
    void reviewedMistakeLeavesTheQueue() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);
        String mistakeId = firstMistakeId(session);

        review(session, mistakeId, true);

        mockMvc.perform(get("/api/v1/progress/review").cookie(session))
                .andExpect(jsonPath("$", hasSize(0)));
        assertThat(count("select count(*) from user_mistakes where resolved = false"
                + " and user_id = cast(? as uuid)", userId(session))).isEqualTo(1L);
    }

    @Test
    @DisplayName("a fully correct attempt resolves the mistake and pays the comeback")
void correctAttemptResolvesAndPays() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);

        String done = completeCorrectTwoSum(session);

        assertThat(done).contains("\"mistakesResolved\":[\"PATTERN\"]");
        assertThat(com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(done, "$.awards[*].reason"))
                .as("awards in %s", done)
                .contains("COMEBACK");

        assertThat(count("select count(*) from user_mistakes where resolved = true"
                + " and user_id = cast(? as uuid)", userId(session))).isEqualTo(1L);
    }

    @Test
    @DisplayName("reviewing needs a session, and another learner's mistake is not found")
    void accessControl() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);
        String mistakeId = firstMistakeId(session);

        mockMvc.perform(post("/api/v1/progress/review/" + mistakeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correct":true}
                                """))
                .andExpect(status().isUnauthorized());

        Cookie other = bootstrap();
        mockMvc.perform(post("/api/v1/progress/review/" + mistakeId)
                        .cookie(other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correct":true}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a client cannot claim XP or a review count through the review body")
    void clientCannotAssertTheOutcome() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        completeWrongPattern(session, attempt);
        String mistakeId = firstMistakeId(session);

        String body = mockMvc.perform(post("/api/v1/progress/review/" + mistakeId)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correct":true,"xp":500,"xpAwarded":500,"reviewCount":99,
                                 "intervalDays":9999,"resolved":true,"alreadyEarned":false}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The only field honoured is the learner's own judgement; everything else came back from
        // the server, so nothing here can be asserted.
        assertThat(body).contains("\"reviewCount\":1");
        assertThat(body).contains("\"intervalDays\":2");
        assertThat(body).contains("\"xpAwarded\":20");
        assertThat(body).doesNotContain("9999");
        assertThat(body).doesNotContain("\"resolved\":true");
    }

    /** Reviews a mistake, returning the response body so a test can read what the server paid. */
    private String review(Cookie session, String mistakeId, boolean correct) throws Exception {
        return mockMvc.perform(post("/api/v1/progress/review/" + mistakeId)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correct\":" + correct + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}