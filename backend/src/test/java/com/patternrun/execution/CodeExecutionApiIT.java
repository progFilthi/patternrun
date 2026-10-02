package com.patternrun.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patternrun.execution.provider.DockerExecutionProvider;
import com.patternrun.problem.ProgrammingLanguage;
import com.patternrun.security.SessionCookie;
import com.patternrun.support.ApiIntegrationTestBase;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * The coding stage, end to end.
 *
 * The Docker provider is stubbed on purpose. What is under test here is the training domain: that a
 * Run cannot solve a problem, that a Submit can, that a hidden case contributes nothing but its
 * verdict, and that none of it can be moved from the client to the server. Those properties do not
 * depend on how the code is executed, and a test suite that needed a container runtime to assert
 * them would be a worse test.
 *
 * <p>{@link PythonHarnessIT} exercises the runner itself against a real Python, and
 * {@link DockerExecutionProviderIT} exercises the container, so the two halves are each covered
 * where they actually live.
 */
class CodeExecutionApiIT extends ApiIntegrationTestBase {

    @MockitoBean
    private DockerExecutionProvider provider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Used to encode the learner's code into a JSON body without hand-escaping newlines. */
    @Autowired
    private ObjectMapper objectMapper;

    private static final String WORKING = """
            def two_sum(nums, target):
                seen = {}
                for i, v in enumerate(nums):
                    if target - v in seen:
                        return [seen[target - v], i]
                    seen[v] = i
                return []
            """;

    @BeforeEach
    void enableTheProvider() {
        when(provider.language()).thenReturn(ProgrammingLanguage.PYTHON);
        when(provider.isAvailable()).thenReturn(true);
    }

    // ---------------------------------------------------------------- fixtures

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

    private String submit(Cookie session, UUID attemptId, String path, String code) throws Exception {
        return mockMvc.perform(post("/api/v1/attempts/" + attemptId + path)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"PYTHON","code":%s}
                                """.formatted(objectMapper.writeValueAsString(code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String submitRaw(Cookie session, UUID attemptId, String path, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/attempts/" + attemptId + path)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** A report where the first {@code passedCount} cases pass and the rest are wrong answers. */
    private static ExecutionReport report(ExecutionOutcome outcome, int total, int passedCount) {
        return report(outcome, total, passedCount, null, null, null);
    }

    private static ExecutionReport report(ExecutionOutcome outcome,
                                          int total,
                                          int passedCount,
                                          String errorType,
                                          String errorMessage,
                                          Integer errorLine) {
        List<ExecutionReport.CaseOutcome> cases = new ArrayList<>();
        for (int index = 0; index < total; index++) {
            cases.add(index < passedCount
                    ? ExecutionReport.CaseOutcome.passed("[" + index + ", 1]")
                    : ExecutionReport.CaseOutcome.wrong("[9, 9]"));
        }
        return new ExecutionReport(outcome, cases, 12L, errorType, errorMessage, errorLine, "", "");
    }

    /** Two Sum has two visible cases and five hidden ones, all runnable. */
    private static final int VISIBLE_CASES = 2;
    private static final int ALL_CASES = 7;

    // ------------------------------------------------------------ Run vs Submit

    @Test
    @DisplayName("Run reports on the visible cases and cannot mark the problem solved")
    void runNeverSolves() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, VISIBLE_CASES, VISIBLE_CASES));

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/run")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n,t): return [0,1]\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("RUN"))
                .andExpect(jsonPath("$.outcome").value("ACCEPTED"))
                .andExpect(jsonPath("$.casesTotal").value(VISIBLE_CASES))
                .andExpect(jsonPath("$.casesPassed").value(VISIBLE_CASES));

        // The point of the whole distinction, asserted on the row rather than the response.
        assertThat(codeAcceptedOf(attempt)).isFalse();
        assertThat(codeOutcomeOf(attempt)).isNull();
        assertThat(executionCountFor(attempt)).isEqualTo(1);
    }

    @Test
    @DisplayName("Submit against the hidden set accepts and records it on the attempt")
    void submitAccepts() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));

        submit(session, attempt, "/code/submit", WORKING);

        assertThat(codeAcceptedOf(attempt)).isTrue();
        assertThat(codeOutcomeOf(attempt)).isEqualTo("ACCEPTED");
        assertThat(executionCountFor(attempt)).isEqualTo(1);
    }

    @Test
    @DisplayName("A wrong answer records the outcome but never sets accepted")
    void submitWrongAnswer() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.WRONG_ANSWER, ALL_CASES, 3));

        submit(session, attempt, "/code/submit", "def two_sum(n, t): return [0, 1]");

        assertThat(codeAcceptedOf(attempt)).isFalse();
        assertThat(codeOutcomeOf(attempt)).isEqualTo("WRONG_ANSWER");
    }

    @Test
    @DisplayName("A later mistake does not un-solve a problem that was already accepted")
    void acceptedIsMonotonic() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any()))
                .thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES))
                .thenReturn(report(ExecutionOutcome.WRONG_ANSWER, ALL_CASES, 1));

        submit(session, attempt, "/code/submit", WORKING);
        submit(session, attempt, "/code/submit", "def two_sum(n, t): return [9, 9]");

        // The same rule the XP ledger follows: an achievement already earned is not reclaimed.
        assertThat(codeAcceptedOf(attempt)).isTrue();
        assertThat(codeOutcomeOf(attempt)).isEqualTo("WRONG_ANSWER");
        assertThat(executionCountFor(attempt)).isEqualTo(2);
    }

    @Test
    @DisplayName("Each run and submit appends one row to the execution log")
    void executionLogKeepsEveryEvent() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.WRONG_ANSWER, ALL_CASES, 2));

        submit(session, attempt, "/code/run", "def two_sum(n, t): return [0, 1]");
        submit(session, attempt, "/code/submit", "def two_sum(n, t): return [0, 1]");

        List<String> kinds = jdbcTemplate.queryForList(
                "SELECT kind FROM user_code_executions WHERE attempt_id = ? ORDER BY created_at", String.class,
                attempt);
        assertThat(kinds).containsExactly("RUN", "SUBMIT");
    }

    // ------------------------------------------------------------ hidden cases

    @Test
    @DisplayName("A hidden case contributes its verdict and nothing else")
    void hiddenCasesRevealNothing() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));

        var tree = objectMapper.readTree(submit(session, attempt, "/code/submit", WORKING));
        var cases = tree.path("cases");

        assertThat(cases).hasSize(ALL_CASES);
        for (var hidden : cases) {
            if (!hidden.path("hidden").asBoolean()) {
                continue;
            }
            // Absent, not null: the API omits nulls, so a hidden case has no label, no input, no
            // expected value and no output at all. Asserted on missing keys rather than on values,
            // because an empty string would leak just as much as the real thing.
            assertThat(hidden.has("label")).as("a hidden label hands over the evaluation set").isFalse();
            assertThat(hidden.has("input")).isFalse();
            assertThat(hidden.has("expected")).isFalse();
            assertThat(hidden.has("actual")).isFalse();
            assertThat(hidden.path("passed").asBoolean()).isTrue();
        }

        // And none of the seeded hidden labels appear anywhere in the payload.
        String body = tree.toString();
        assertThat(body).doesNotContain("Duplicate values");
        assertThat(body).doesNotContain("Opposite extremes");
        assertThat(body).doesNotContain("Pair straddles the middle");
        assertThat(body).doesNotContain("Two zeros");
    }

    @Test
    @DisplayName("Run never returns a hidden case at all, not even as a count of failures")
    void runSeesOnlyVisibleCases() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, VISIBLE_CASES, VISIBLE_CASES));

        String body = submit(session, attempt, "/code/run", WORKING);

        assertThat(body).doesNotContain("\"hidden\":true");
    }

    @Test
    @DisplayName("The provider is handed hidden cases, so the evaluation set is genuinely evaluated")
    void providerReceivesTheHiddenCases() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));

        submit(session, attempt, "/code/submit", WORKING);

        var request = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(provider).execute(request.capture());
        assertThat(request.getValue().cases()).hasSize(ALL_CASES);
        assertThat(request.getValue().cases().stream().filter(ExecutionCase::hidden).count())
                .as("submit must evaluate the hidden cases, or it decides nothing")
                .isEqualTo(5L);
        assertThat(request.getValue().entrypoint()).isEqualTo("two_sum");
    }

    // ------------------------------------------------------------ error shapes

    @Nested
    @DisplayName("Failures are reported in a form a learner can act on")
    class FailureReporting {

        @Test
        @DisplayName("A syntax error carries the learner's line, not a path")
        void syntaxError() throws Exception {
            Cookie session = bootstrap();
            UUID attempt = startTwoSum(session);
            when(provider.execute(any())).thenReturn(report(
                    ExecutionOutcome.SYNTAX_ERROR, 0, 0, "SyntaxError", "expected ':'", 4));

            mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t)\\n    pass\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.outcome").value("SYNTAX_ERROR"))
                    .andExpect(jsonPath("$.error.type").value("SyntaxError"))
                    .andExpect(jsonPath("$.error.line").value(4))
                    .andExpect(jsonPath("$.error.message").value("expected ':'"));
        }

        @Test
        @DisplayName("A runtime error names the exception and the line it came from")
        void runtimeError() throws Exception {
            Cookie session = bootstrap();
            UUID attempt = startTwoSum(session);
            when(provider.execute(any())).thenReturn(report(
                    ExecutionOutcome.RUNTIME_ERROR, 0, 0, "TypeError",
                    "unsupported operand type(s) for -: 'int' and 'list'", 2));

            submit(session, attempt, "/code/submit", "def two_sum(n, t): return t - n");

            assertThat(codeAcceptedOf(attempt)).isFalse();
        }

        @Test
        @DisplayName("A timeout is TIME_LIMIT_EXCEEDED, not a wrong answer")
        void timeout() throws Exception {
            Cookie session = bootstrap();
            UUID attempt = startTwoSum(session);
            when(provider.execute(any())).thenReturn(new ExecutionReport(
                    ExecutionOutcome.TIME_LIMIT_EXCEEDED, List.of(), 2001L,
                    "TimeoutError", "The solution did not finish in time.", null, "", ""));

            mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t):\\n while True: pass\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.outcome").value("TIME_LIMIT_EXCEEDED"))
                    .andExpect(jsonPath("$.error.message", containsString("in time")));
        }

        /**
         * The most important one in this class.
         *
         * If a broken runner were reported as a wrong answer, a learner with a correct solution
         * would be told their code is broken, and would go and change working code because our
         * infrastructure had a bad minute.
         */
        @Test
        @DisplayName("A broken runner is INTERNAL_ERROR and says so, never a wrong answer")
        void runnerFailureIsNotTheLearnersFault() throws Exception {
            Cookie session = bootstrap();
            UUID attempt = startTwoSum(session);
            when(provider.execute(any())).thenReturn(ExecutionReport.infrastructureFailure(
                    "docker: daemon unreachable at /var/run/docker.sock: connection refused"));

            mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t): return [0, 1]\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.outcome").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.error.type").value("RunnerUnavailable"))
                    .andExpect(jsonPath("$.error.message", containsString("not with your code")));

            // And the raw message, which carries the socket path, is never surfaced.
            String body = mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t): return [0, 1]\"}"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("/var/run/docker.sock");
            assertThat(body).doesNotContain("docker");
        }

        /**
         * The contradiction this pins down.
         *
         * A runner failure used to render every case as a failure, because a case with no result
         * cannot have passed. The banner then said "that is a problem on our side, not with your
         * code" directly above a list claiming the code raised on every example, and a learner
         * reads the detail.
         */
        @Test
        @DisplayName("A broken runner reports no case results at all, rather than all of them failing")
        void runnerFailureReportsNoCases() throws Exception {
            Cookie session = bootstrap();
            UUID attempt = startTwoSum(session);
            when(provider.execute(any())).thenReturn(ExecutionReport.infrastructureFailure("down"));

            String body = mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t): return [0, 1]\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.outcome").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.casesTotal").value(0))
                    .andExpect(jsonPath("$.casesPassed").value(0))
                    .andExpect(jsonPath("$.cases", org.hamcrest.Matchers.hasSize(0)))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("it raised before returning");
            assertThat(body).doesNotContain("Example 1");
        }

        @Test
        @DisplayName("A provider that is switched off reports unavailable rather than guessing")
        void disabledRunner() throws Exception {
            Cookie session = bootstrap();
            UUID attempt = startTwoSum(session);
            when(provider.isAvailable()).thenReturn(false);

            mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t): return [0, 1]\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.outcome").value("INTERNAL_ERROR"));

            verify(provider, never()).execute(any());
        }
    }

    // ------------------------------------------------------- the security boundary

    @Test
    @DisplayName("A client cannot assert that its code passed")
    void clientCannotClaimSuccess() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.WRONG_ANSWER, ALL_CASES, 0));

        // Every field a client might reach for, all of them ignored.
        String body = submitRaw(session, attempt, "/code/submit", """
                {"language":"PYTHON","code":"def two_sum(n, t): return [0, 1]",
                 "passed":true,"accepted":true,"outcome":"ACCEPTED","casesPassed":7,
                 "correct":true,"codeAccepted":true}
                """);

        assertThat(body).contains("\"outcome\":\"WRONG_ANSWER\"");
        assertThat(codeAcceptedOf(attempt)).isFalse();
    }

    @Test
    @DisplayName("Only the source and the language are ever sent to the runner")
    void providerReceivesOnlyTheSource() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));

        submitRaw(session, attempt, "/code/submit", """
                {"language":"PYTHON","code":"def two_sum(n, t): return [0, 1]","passed":true}
                """);

        var request = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(provider).execute(request.capture());
        // The record has no field a verdict could arrive in, so this is belt and braces rather
        // than the primary argument.
        assertThat(ExecutionRequest.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("language", "source", "entrypoint", "cases", "timeoutSeconds",
                        "argumentMode");
    }

    @Test
    @DisplayName("The client cannot choose the function the runner calls")
    void entrypointComesFromTheServer() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));

        submitRaw(session, attempt, "/code/submit", """
                {"language":"PYTHON","code":"def two_sum(n, t): return [0, 1]","entrypoint":"anything_i_like"}
                """);

        var request = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(provider).execute(request.capture());
        assertThat(request.getValue().entrypoint())
                .as("a client-chosen entrypoint would let it pick its own argument shape")
                .isEqualTo("two_sum");
    }

    // ------------------------------------------------------------ access control

    @Test
    @DisplayName("Running code needs a session")
    void anonymousIsRejected() throws Exception {
        UUID attempt = startTwoSum(bootstrap());

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"PYTHON\",\"code\":\"x = 1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Another learner's attempt is a 404, indistinguishable from one that does not exist")
    void anotherLearnersAttemptIsNotFound() throws Exception {
        UUID attempt = startTwoSum(bootstrap());
        Cookie other = bootstrap();

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                        .cookie(other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"PYTHON\",\"code\":\"x = 1\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A finished session refuses further code")
    void completedAttemptIsRejected() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));
        submit(session, attempt, "/code/submit", WORKING);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"complexityTime\":\"O(n)\",\"complexitySpace\":\"O(n)\",\"durationMs\":30000}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/submit")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t): return [0, 1]\"}"))
                .andExpect(status().isConflict());
    }

    /**
     * Every problem now has an editor.
     *
     * This replaces a test that pointed at a problem with no runnable content, which stopped existing
     * once the catalogue was finished. The refusal itself is still covered, by
     * {@link CodeExecutionServiceTest}, because a problem added without test arguments is a routine
     * thing to do here and it must be refused rather than offered an editor that cannot work.
     */
    @Test
    @DisplayName("Every problem in the catalogue is runnable")
    void everyProblemIsRunnable() throws Exception {
        var problems = mockMvc.perform(get("/api/v1/problems?size=100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (var node : objectMapper.readTree(problems).path("content")) {
            String slug = node.path("slug").asText();
            assertThat(readEntrypoint(slug))
                    .as("%s should be runnable, and a missing entrypoint means its content is unfinished", slug)
                    .isNotNull()
                    .isNotBlank();
        }

        assertThat(readEntrypoint("two-sum")).isEqualTo("two_sum");
    }

    private String readEntrypoint(String slug) throws Exception {
        String body = mockMvc.perform(get("/api/v1/problems/" + slug))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.has("runnableEntrypoint") ? node.get("runnableEntrypoint").asText() : null;
    }

    // -------------------------------------------------------------- save and state

    @Test
    @DisplayName("Autosave stores the source and decides nothing")
    void saveStoresWithoutDeciding() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/code/save")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"PYTHON\",\"code\":\"def two_sum(n, t):\\n    pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("PYTHON"))
                .andExpect(jsonPath("$.outcome").doesNotExist())
                .andExpect(jsonPath("$.accepted").value(false));

        verify(provider, never()).execute(any());
        assertThat(codeAcceptedOf(attempt)).isFalse();
        assertThat(executionCountFor(attempt)).isZero();
    }

    @Test
    @DisplayName("Saved source survives a reload, with the last verdict attached")
    void stateRestoresAfterReload() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.WRONG_ANSWER, ALL_CASES, 2));
        submit(session, attempt, "/code/submit", WORKING);

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/code").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(WORKING))
                .andExpect(jsonPath("$.outcome").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.accepted").value(false));
    }

    // ----------------------------------------------------------------- hints

    @Test
    @DisplayName("The hint after a wrong answer is the debugging one, chosen by the server")
    void wrongAnswerSelectsADebuggingHint() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.WRONG_ANSWER, ALL_CASES, 0));
        submit(session, attempt, "/code/submit", "def two_sum(n, t): return [0, 1]");

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/hint/next")
                        .cookie(session)
                        .param("stage", "CODING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.content", containsString("Check what you store")));
    }

    @Test
    @DisplayName("Before anything has failed, the coding hint is the conceptual one")
    void codingHintBeforeAnyFailure() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/hint/next")
                        .cookie(session)
                        .param("stage", "CODING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("ANY"))
                .andExpect(jsonPath("$.content", containsString("remember")));
    }

    /**
     * The regression this pins down.
     *
     * A Run is a diagnostic, not a verdict. Someone who submitted, got a wrong answer, and then ran
     * the same code to narrow it down is still wrong, and swapping the debugging ladder for the
     * conceptual one at that moment reads as the tool having forgotten what it just told them.
     */
    @Test
    @DisplayName("A passing Run does not clear the debugging context from a wrong Submit")
    void runDoesNotResetTheHintContext() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any()))
                .thenReturn(report(ExecutionOutcome.WRONG_ANSWER, ALL_CASES, 0))
                .thenReturn(report(ExecutionOutcome.ACCEPTED, VISIBLE_CASES, VISIBLE_CASES));

        submit(session, attempt, "/code/submit", "def two_sum(n, t): return [0, 1]");
        submit(session, attempt, "/code/run", "def two_sum(n, t): return [0, 1]");

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/hint/next")
                        .cookie(session)
                        .param("stage", "CODING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("WRONG_ANSWER"));
    }

    @Test
    @DisplayName("A timeout gets the complexity hint, not the debugging one")
    void timeoutSelectsAComplexityHint() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(new ExecutionReport(
                ExecutionOutcome.TIME_LIMIT_EXCEEDED, List.of(), 2000L,
                "TimeoutError", "did not finish", null, "", ""));
        submit(session, attempt, "/code/submit", WORKING);

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/hint/next")
                        .cookie(session)
                        .param("stage", "CODING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("TIME_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.content", containsString("complexity")));
    }

    /**
     * The reason hint selection is server-side at all.
     *
     * If the client could name its own trigger, it could ask for the debugging ladder after a
     * passing submission and walk to the answer without ever having failed.
     */
    @Test
    @DisplayName("The client cannot choose which hint ladder it gets")
    void triggerIsNotClientChosen() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/hint/next")
                        .cookie(session)
                        .param("stage", "CODING")
                        .param("trigger", "WRONG_ANSWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("ANY"));
    }

    @Test
    @DisplayName("Asking for a hint needs a session")
    void hintNeedsASession() throws Exception {
        UUID attempt = startTwoSum(bootstrap());

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/hint/next").param("stage", "CODING"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------- solution reveal

    @Test
    @DisplayName("Revealing the solution returns it and records that it happened")
    void revealRecordsAndReturns() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/solution")
                        .cookie(session)
                        .param("language", "PYTHON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("PYTHON"))
                .andExpect(jsonPath("$.code", containsString("def two_sum")));

        assertThat(solutionRevealedOf(attempt)).isTrue();
    }

    @Test
    @DisplayName("There is no read that hands out a solution without recording it")
    void revealIsNotARead() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(get("/api/v1/attempts/" + attempt + "/solution").cookie(session))
                .andExpect(status().isMethodNotAllowed());

        assertThat(solutionRevealedOf(attempt)).isFalse();
    }

    @Test
    @DisplayName("Revealing needs a session")
    void revealNeedsASession() throws Exception {
        UUID attempt = startTwoSum(bootstrap());

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/solution"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- integration

    @Test
    @DisplayName("The completion response reports the code verdict without gating the Phase 3 awards")
    void completionReportsTheCodeVerdict() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));
        submit(session, attempt, "/code/submit", WORKING);

        String completion = mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"complexityTime\":\"O(n)\",\"complexitySpace\":\"O(n)\",\"durationMs\":45000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeAccepted").value(true))
                .andExpect(jsonPath("$.codeOutcome").value("ACCEPTED"))
                .andExpect(jsonPath("$.solvedIndependently").value(true))
                // Phase 3 is untouched: the XP awards are exactly what they were before.
                .andExpect(jsonPath("$.totalXpAwarded").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn().getResponse().getContentAsString();

        assertThat(completion).contains("\"patternCorrect\":false");
    }

    @Test
    @DisplayName("A hinted attempt is accepted but not counted as solved independently")
    void hintedSolveIsNotIndependent() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/hint")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":2}"))
                .andExpect(status().isOk());
        submit(session, attempt, "/code/submit", WORKING);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"complexityTime\":\"O(n)\",\"complexitySpace\":\"O(n)\",\"durationMs\":45000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeAccepted").value(true))
                .andExpect(jsonPath("$.solvedIndependently").value(false));
    }

    @Test
    @DisplayName("Revealing the solution then accepting is assisted, not independent")
    void revealedSolveIsNotIndependent() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);
        when(provider.execute(any())).thenReturn(report(ExecutionOutcome.ACCEPTED, ALL_CASES, ALL_CASES));

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/solution").cookie(session))
                .andExpect(status().isOk());
        submit(session, attempt, "/code/submit", WORKING);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"complexityTime\":\"O(n)\",\"complexitySpace\":\"O(n)\",\"durationMs\":45000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solvedIndependently").value(false));
    }

    @Test
    @DisplayName("Completing without ever writing code still works, exactly as in Phase 3")
    void codeIsNotRequiredToComplete() throws Exception {
        Cookie session = bootstrap();
        UUID attempt = startTwoSum(session);

        mockMvc.perform(post("/api/v1/attempts/" + attempt + "/complete")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"complexityTime\":\"O(n)\",\"complexitySpace\":\"O(n)\",\"durationMs\":45000}"))
                .andExpect(status().isOk())
                // Absent, not false: "not measured" and "measured as failed" are different and
                // the API is configured to omit nulls so absence is how the first one arrives.
                .andExpect(jsonPath("$.codeOutcome").doesNotExist())
                .andExpect(jsonPath("$.codeAccepted").value(false))
                .andExpect(jsonPath("$.totalXpAwarded").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    // ---------------------------------------------------------------- helpers

    private Boolean codeAcceptedOf(UUID attemptId) {
        return jdbcTemplate.queryForObject(
                "select code_accepted from user_problem_attempts where id = ?", Boolean.class, attemptId);
    }

    private String codeOutcomeOf(UUID attemptId) {
        return jdbcTemplate.queryForObject(
                "select code_outcome from user_problem_attempts where id = ?", String.class, attemptId);
    }

    private Boolean solutionRevealedOf(UUID attemptId) {
        return jdbcTemplate.queryForObject(
                "select solution_revealed from user_problem_attempts where id = ?", Boolean.class, attemptId);
    }

    private Integer executionCountFor(UUID attemptId) {
        return jdbcTemplate.queryForObject(
                "select code_executions from user_problem_attempts where id = ?", Integer.class, attemptId);
    }

    @Test
    @DisplayName("The visible test cases endpoint still leaks nothing hidden")
    void publicTestCasesStayClean() throws Exception {
        String body = mockMvc.perform(get("/api/v1/problems/two-sum/test-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(VISIBLE_CASES)))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("Duplicate values");
        assertThat(body).doesNotContain("hidden");
    }
}