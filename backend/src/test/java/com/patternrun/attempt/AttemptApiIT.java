package com.patternrun.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * XP, anti-abuse and the trust boundary, end to end against a real database.
 *
 * These are the promises the game layer makes, so they are tested against the database rather
 * than against a service with mocked repositories. The unique index on the award key is the
 * mechanism that enforces them, and a mock would happily let a duplicate through.
 */
class AttemptApiIT extends ApiIntegrationTestBase {

    /**
     * The QUESTION step of two-sum, and its answer, read from the shipped content rather than
     * assumed. If the seed ever changes, this test fails loudly rather than testing a fiction.
     */
    private static final int TWO_SUM_QUESTION_ORDER = 3;
    private static final int TWO_SUM_CORRECT_OPTION = 1;

    @Autowired
    private ObjectMapper objectMapper;

    /** Read-only, to assert on the rows the ledger and its unique index actually produced. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Cookie bootstrap() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/session")).andReturn();
        return result.getResponse().getCookie(SessionCookie.NAME);
    }

    private JsonNode postJson(Cookie session, String path, String body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private UUID startTwoSum(Cookie session) throws Exception {
        return UUID.fromString(postJson(session, "/api/v1/attempts", """
                {"problemSlug":"two-sum","mode":"STANDARD"}
                """).get("id").asText());
    }

    private void predict(Cookie session, UUID attempt, int stepOrder, int chosen) throws Exception {
        postJson(session, "/api/v1/attempts/" + attempt + "/predict", """
                {"stepOrder":%d,"chosenIndex":%d}
                """.formatted(stepOrder, chosen));
    }

    /** Makes the whole loop correct: the right pattern, then the right prediction. */
    private void answerTheQuestionCorrectly(Cookie session, UUID attempt) throws Exception {
        postJson(session, "/api/v1/attempts/" + attempt + "/pattern", """
                {"patternSlug":"hashing"}
                """);
        predict(session, attempt, TWO_SUM_QUESTION_ORDER, TWO_SUM_CORRECT_OPTION);
    }

    private JsonNode complete(Cookie session, UUID attempt, String body) throws Exception {
        return postJson(session, "/api/v1/attempts/" + attempt + "/complete", body);
    }

    private String learnerIdOf(Cookie session) throws Exception {
        String me = mockMvc.perform(get("/api/v1/auth/me").cookie(session))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(me).get("user").get("id").asText();
    }

    private long countAwards(String userId, String reason) {
        Long value = jdbcTemplate.queryForObject(
                "select count(*) from user_xp_awards where user_id = cast(? as uuid) and reason = ?",
                Long.class,
                userId,
                reason);
        return value == null ? 0 : value;
    }

    @Test
    @DisplayName("a clean session pays out every award it earned")
    void cleanSessionPaysOut() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        answerTheQuestionCorrectly(session, attempt);

        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":180000}
                """);

        assertThat(result.get("totalXpAwarded").asInt()).isPositive();
        assertThat(result.get("grade").asText()).isNotBlank();
        assertThat(result.get("combo").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(result.get("patternSlug").asText()).isEqualTo("hashing");
        assertThat(result.get("predictionsCorrect").asInt()).isEqualTo(1);
        // The breakdown tells the client which rules fired, not just a total.
        assertThat(result.get("awards").size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("solving the same problem many times pays the completion award once")
    void repeatSolvesPayOnce() throws Exception {
        Cookie session = bootstrap();
        String userId = learnerIdOf(session);

        UUID first = startTwoSum(session);
        answerTheQuestionCorrectly(session, first);
        int firstTotal = complete(session, first, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":180000}
                """).get("totalXpAwarded").asInt();

        for (int i = 0; i < 4; i++) {
            UUID repeat = startTwoSum(session);
            answerTheQuestionCorrectly(session, repeat);
            complete(session, repeat, """
                    {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                     "durationMs":180000}
                    """);
        }

        // This is the anti-abuse promise: five solved sessions, one completion award.
        assertThat(firstTotal).isPositive();
        assertThat(countAwards(userId, "PROBLEM_COMPLETED"))
                .as("grinding one problem must not pay the completion award again")
                .isEqualTo(1);
        assertThat(countAwards(userId, "NO_HINTS"))
                .as("each of the other once-per-problem awards is also spent on the first solve")
                .isEqualTo(1);
        assertThat(countAwards(userId, "PATTERN_IDENTIFIED")).isEqualTo(1);
        assertThat(countAwards(userId, "PREDICTION_CORRECT"))
                .as("predicting correctly teaches once; the rate is measured in mastery instead")
                .isEqualTo(1);

        // And the whole repeat sequence must be worth nothing at all.
        int repeatTotal = 0;
        for (int i = 0; i < 4; i++) {
            UUID repeat = startTwoSum(session);
            answerTheQuestionCorrectly(session, repeat);
            repeatTotal += complete(session, repeat, """
                    {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                     "durationMs":180000}
                    """).get("totalXpAwarded").asInt();
        }
        assertThat(repeatTotal)
                .as("four more perfect solves of the same problem must pay nothing")
                .isZero();
    }

    @Test
    @DisplayName("a run through new problems grows the combo bonus")
    void comboGrowsAcrossNewProblems() throws Exception {
        Cookie session = bootstrap();

        // Each of these is a first solve, so the run bonus applies and scales.
        int previousXp = 0;
        for (String slug : new String[] {"two-sum", "climbing-stairs", "maximum-depth-of-binary-tree"}) {
            String pattern = switch (slug) {
                case "two-sum" -> "hashing";
                case "climbing-stairs" -> "dp";
                default -> "trees";
            };
            UUID attempt = UUID.fromString(postJson(session, "/api/v1/attempts", """
                    {"problemSlug":"%s","mode":"STANDARD"}
                    """.formatted(slug)).get("id").asText());

            postJson(session, "/api/v1/attempts/" + attempt + "/pattern", """
                    {"patternSlug":"%s"}
                    """.formatted(pattern));

            int xp = complete(session, attempt, """
                    {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                     "durationMs":180000}
                    """).get("totalXpAwarded").asInt();

            assertThat(xp)
                    .as("a longer run should pay more for %s", slug)
                    .isGreaterThan(previousXp);
            previousXp = xp;
        }
    }

    @Test
    @DisplayName("a pattern guess is judged against the problem, not accepted from the client")
    void patternIsJudgedServerSide() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        // The wrong pattern. The client sends a slug and cannot assert that it was right.
        postJson(session, "/api/v1/attempts/" + attempt + "/pattern", """
                {"patternSlug":"monotonic-stack"}
                """);

        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":180000}
                """);

        assertThat(result.get("patternCorrect").asBoolean()).isFalse();
        assertThat(countAwards(learnerIdOf(session), "PATTERN_IDENTIFIED")).isZero();
    }

    @Test
    @DisplayName("a correct pattern pays the identification award exactly once")
    void correctPatternPaysOnce() throws Exception {
        Cookie session = bootstrap();
        String userId = learnerIdOf(session);

        UUID attempt = startTwoSum(session);
        answerTheQuestionCorrectly(session, attempt);
        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":180000}
                """);

        assertThat(result.get("patternCorrect").asBoolean()).isTrue();
        assertThat(countAwards(userId, "PATTERN_IDENTIFIED")).isEqualTo(1);
    }

    @Test
    @DisplayName("a retried completion cannot double-credit")
    void retriedCompletionIsSafe() throws Exception {
        Cookie session = bootstrap();
        String userId = learnerIdOf(session);
        UUID attempt = startTwoSum(session);
        answerTheQuestionCorrectly(session, attempt);

        String body = """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":180000}
                """;
        int firstTotal = complete(session, attempt, body).get("totalXpAwarded").asInt();
        long awardsAfterFirst = countAwards(userId, "PROBLEM_COMPLETED");

        // A client that times out and retries, which is the ordinary mobile failure.
        int retryTotal = complete(session, attempt, body).get("totalXpAwarded").asInt();

        assertThat(retryTotal).isEqualTo(firstTotal);
        assertThat(countAwards(userId, "PROBLEM_COMPLETED")).isEqualTo(awardsAfterFirst);
    }

    @Test
    @DisplayName("a prediction is marked wrong when the client picked the wrong option")
    void predictionIsJudgedServerSide() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        // The wrong option on purpose. The client only sends an index, so it has no way to
        // claim otherwise: the answer is already inside the animation payload it received.
        predict(session, attempt, TWO_SUM_QUESTION_ORDER, 0);

        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":180000}
                """);

        assertThat(result.get("predictionsAnswered").asInt()).isEqualTo(1);
        assertThat(result.get("predictionsCorrect").asInt())
                .as("a wrong option must be recorded as wrong")
                .isZero();
    }

    @Test
    @DisplayName("complexity is judged against the problem, not accepted from the client")
    void complexityIsJudgedServerSide() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n^2)","complexitySpace":"O(1)","breakdownCorrect":false,
                 "durationMs":120000}
                """);

        assertThat(result.get("complexityCorrect").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("a superscript option matches a caret answer")
    void complexityNormalisation() throws Exception {
        // 3sum is the seeded problem whose expected time is written "O(n^2)". The only option
        // the learner can pick is written "O(n²)". Before the two spellings were folded
        // together this was unreachable, so the right answer was always marked wrong.
        Cookie session = bootstrap();
        UUID attempt = UUID.fromString(postJson(session, "/api/v1/attempts", """
                {"problemSlug":"3sum","mode":"STANDARD"}
                """).get("id").asText());

        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n²)","complexitySpace":"O(1)","breakdownCorrect":true,
                 "durationMs":120000}
                """);

        assertThat(result.get("complexityCorrect").asBoolean())
                .as("the caret and the superscript are the same complexity")
                .isTrue();
    }

    @Test
    @DisplayName("an implausible duration is dropped rather than poisoning the speed axis")
    void implausibleDurationIsDropped() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":7}
                """);

        assertThat(result.has("durationMs"))
                .as("an omitted field, not a zero: Jackson is configured to drop nulls")
                .isFalse();
        assertThat(result.get("provisional").asBoolean())
                .as("a dropped duration leaves the grade provisional rather than quietly wrong")
                .isTrue();
    }

    @Test
    @DisplayName("hints used is recorded, so hint dependency is evidence")
    void hintsAreRecorded() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        postJson(session, "/api/v1/attempts/" + attempt + "/hint", """
                {"level":1}
                """);
        postJson(session, "/api/v1/attempts/" + attempt + "/hint", """
                {"level":3}
                """);

        JsonNode result = complete(session, attempt, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true,
                 "durationMs":180000}
                """);

        assertThat(result.get("hintsUsed").asInt())
                .as("the deepest rung revealed is what counts, not how many clicks it took")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("a finished session cannot be written to again")
    void finishedAttemptsAreClosed() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        complete(session, attempt, """
                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true}
                """);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/hint")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"level":2}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("one learner's session is invisible to another")
    void attemptsAreScopedToTheirOwner() throws Exception {
        Cookie mine = bootstrap();
        UUID attempt = startTwoSum(mine);
        Cookie theirs = bootstrap();

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(theirs)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"complexityTime":"O(n)","complexitySpace":"O(n)","breakdownCorrect":true}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an attempt endpoint without a session is 401")
    void attemptsRequireASession() throws Exception {
        mockMvc.perform(post("/api/v1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemSlug":"two-sum"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an unknown problem slug is a 404")
    void unknownProblem() throws Exception {
        Cookie session = bootstrap();

        mockMvc.perform(post("/api/v1/attempts")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemSlug":"no-such-problem"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("starting twice hands back the same live session")
    void startIsIdempotent() throws Exception {
        Cookie session = bootstrap();

        UUID first = startTwoSum(session);
        UUID second = startTwoSum(session);

        assertThat(second)
                .as("a refresh mid-session should resume it, not fail")
                .isEqualTo(first);
    }

    @Test
    @DisplayName("a prediction for a step with no question is a 404")
    void predictingOnANonQuestionStep() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/predict")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepOrder":1,"chosenIndex":0}
                                """))
                .andExpect(status().isNotFound());
    }
}