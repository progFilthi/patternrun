package com.patternrun.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Every published answer, checked by running the reference solution.
 *
 * Nine of the twenty problems shipped wrong answers. The diameter example said 3 for a tree whose
 * diameter is 4; an island grid said 3 where the grid contains 2; a minimum window was labelled
 * with a valid but non-minimal string; a stock span list was written in reverse; a subarray count
 * was off by two. In several cases the prose explanation contradicted its own answer, which is how
 * you can tell it was never executed.
 *
 * Nothing caught them because nothing ran. The answers were only ever rendered as text, so "is
 * this right" was a question a human answered once, by eye, and got wrong nine times.
 *
 * <p>This test makes it a question the build answers. It is the guard that turned a one-time manual
 * check into a property of the repository.
 *
 * <p>Skipped without a Python interpreter, so the suite still passes on a machine that has none.
 */
@EnabledIf("pythonIsAvailable")
class ReferenceSolutionsIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unused") // referenced by @EnabledIf
    static boolean pythonIsAvailable() {
        try {
            Process probe = new ProcessBuilder("python3", "--version")
                    .redirectErrorStream(true)
                    .start();
            return probe.waitFor(10, java.util.concurrent.TimeUnit.SECONDS) && probe.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Test
    @DisplayName("Every runnable problem's reference solution passes every one of its own test cases")
    void everyReferenceSolvesEveryCase() throws Exception {
        Path harness = Files.createTempFile("patternrun-harness", ".py");
        Path problems = Path.of("src/main/resources/seed/problems");

        List<String> problemsChecked = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        int casesChecked = 0;

        try (var stream = getClass().getResourceAsStream("/code-execution/python-harness.py")) {
            assertThat(stream).as("the harness must be on the classpath").isNotNull();
            Files.write(harness, stream.readAllBytes());
        }

        try (var files = Files.list(problems)) {
            for (Path file : files.sorted().toList()) {
                JsonNode problem = MAPPER.readTree(Files.readString(file));

                // Two Sum is covered by CodeExecutionApiIT against a stubbed provider, and by the
                // Docker provider test end to end. Checking it here too costs a second per case.
                if (problem.path("entrypoint").isMissingNode()) {
                    continue;
                }

                String slug = problem.path("slug").asText();
                String entrypoint = problem.path("entrypoint").asText();
                StringBuilder source = new StringBuilder();
                for (JsonNode solution : problem.path("solutions")) {
                    if (solution.path("language").asText().equals("PYTHON")) {
                        source.append(solution.path("code").asText());
                    }
                }
                if (source.isEmpty()) {
                    failures.add(slug + ": runnable but has no Python reference solution");
                    continue;
                }

                var cases = problem.path("testCases");
                var arguments = MAPPER.createArrayNode();
                var expected = MAPPER.createArrayNode();
                for (JsonNode testCase : cases) {
                    arguments.add(testCase.path("call"));
                    expected.add(testCase.path("expected"));
                }

                JsonNode result = run(harness, source.toString(), entrypoint,
                        problem.path("argumentMode").asText("PLAIN"), arguments, expected);
                problemsChecked.add(slug);
                casesChecked += cases.size();

                if (!"COMPLETED".equals(result.path("status").asText())) {
                    failures.add(slug + ": the reference did not run at all -> "
                            + result.path("error").path("message").asText("no message"));
                    continue;
                }
                var results = result.path("cases");
                for (int index = 0; index < cases.size(); index++) {
                    if (!results.path(index).path("passed").asBoolean(false)) {
                        JsonNode testCase = cases.path(index);
                        failures.add(slug + ": case \"" + testCase.path("label").asText()
                                + "\" -> the reference computed " + results.path(index).path("actual")
                                .asText("nothing") + " but the content publishes "
                                + testCase.path("expectedOutput").asText());
                    }
                }
            }
        } finally {
            Files.deleteIfExists(harness);
        }

        assertThat(problemsChecked)
                .as("every runnable problem should have been checked, or this test is vacuous")
                .hasSizeGreaterThanOrEqualTo(20);
        assertThat(failures).isEmpty();
        assertThat(casesChecked).as("every case should have been exercised").isGreaterThanOrEqualTo(100);
    }

    /**
     * The two human-readable and machine-readable forms must not drift apart.
     *
     * A learner reads {@code expectedOutput} and the runner uses {@code expected}. Two copies of one
     * answer is two chances to be wrong, and nothing else in the build compares them.
     */
    @Test
    @DisplayName("The published answer and the runnable answer are the same value")
    void displayAndStructuredAnswersAgree() throws Exception {
        Path problems = Path.of("src/main/resources/seed/problems");
        List<String> drift = new ArrayList<>();

        try (var files = Files.list(problems)) {
            for (Path file : files.sorted().toList()) {
                JsonNode problem = MAPPER.readTree(Files.readString(file));
                if (problem.path("entrypoint").isMissingNode()) {
                    continue;
                }
                for (JsonNode testCase : problem.path("testCases")) {
                    String shown = testCase.path("expectedOutput").asText();
                    String computed = display(testCase.path("expected"));
                    if (!shown.equals(computed)) {
                        drift.add(problem.path("slug").asText() + ": shows " + shown
                                + ", runs against " + computed);
                    }
                }
            }
        }

        assertThat(drift).isEmpty();
    }

    /**
     * An example the problem statement shows must agree with the case that repeats it.
     *
     * Two copies of the same worked example, corrected independently, is how number-of-islands
     * ended up saying 3 in the prose and 2 in the tests.
     */
    @Test
    @DisplayName("A worked example agrees with the test case that repeats it")
    void examplesAgreeWithTheirCases() throws Exception {
        Path problems = Path.of("src/main/resources/seed/problems");
        List<String> drift = new ArrayList<>();

        try (var files = Files.list(problems)) {
            for (Path file : files.sorted().toList()) {
                JsonNode problem = MAPPER.readTree(Files.readString(file));
                Map<String, String> byInput = new java.util.HashMap<>();
                for (JsonNode testCase : problem.path("testCases")) {
                    byInput.put(normalise(testCase.path("input").asText()),
                            testCase.path("expectedOutput").asText());
                }
                for (JsonNode example : problem.path("examples")) {
                    String key = normalise(example.path("input").asText());
                    String answer = byInput.get(key);
                    if (answer != null && !normalise(answer).equals(normalise(example.path("output").asText()))) {
                        drift.add(problem.path("slug").asText() + ": the example says "
                                + example.path("output").asText() + " and the case says " + answer);
                    }
                }
            }
        }

        assertThat(drift).isEmpty();
    }

    /**
     * A problem whose answer is not unique cannot be graded.
     *
     * Top K Frequent Elements has a case where two values tie, so returning either is correct. The
     * harness compares structurally, which means a learner who returns the other tied value is
     * told they are wrong while having written something just as correct. That is the worst kind
     * of false negative, so ties are broken explicitly in the content and the reference documents
     * why.
     */
    @Test
    @DisplayName("Answers that depend on tie-breaking say so, rather than punishing a valid answer")
    void ambiguousAnswersAreAvoidedOrDocumented() throws Exception {
        Path problems = Path.of("src/main/resources/seed/problems");
        JsonNode topK = MAPPER.readTree(Files.readString(problems.resolve("top-k-frequent-elements.json")));

        String reference = topK.path("solutions").get(1).path("code").asText();
        assertThat(reference)
                .as("the reference has to explain its tie-break, or the next author will not know why")
                .containsIgnoringCase("tie");

        // The seeded tie case must be one where the smaller value is named, so the answer is fixed.
        JsonNode tieCase = topK.path("testCases").get(2);
        assertThat(tieCase.path("expectedOutput").asText()).isEqualTo("[4]");
    }

    private static String normalise(String value) {
        return value.replaceAll("\\s+", "");
    }

    /** Renders a JSON value the way the content writes it: no spaces inside brackets. */
    private static String display(JsonNode value) {
        if (value.isTextual()) {
            return "\"" + value.asText() + "\"";
        }
        if (value.isArray()) {
            StringBuilder out = new StringBuilder("[");
            for (int index = 0; index < value.size(); index++) {
                if (index > 0) {
                    out.append(",");
                }
                out.append(display(value.get(index)));
            }
            return out.append("]").toString();
        }
        return value.asText();
    }

    /** Runs one reference against every case at once, through the shipped harness. */
    private JsonNode run(Path harness, String source, String entrypoint, String argumentMode,
                         JsonNode arguments, JsonNode expected) throws Exception {
        var envelope = MAPPER.createObjectNode();
        envelope.put("source", source);
        envelope.put("entrypoint", entrypoint);
        // The calling convention matters here: without it the tree problems look broken to this
        // test rather than to a learner.
        envelope.put("argumentMode", argumentMode);
        envelope.put("timeoutSeconds", 5.0);
        var cases = MAPPER.createArrayNode();
        for (int index = 0; index < arguments.size(); index++) {
            var single = MAPPER.createObjectNode();
            single.put("label", "case-" + index);
            single.set("args", arguments.get(index));
            single.set("expected", expected.get(index));
            cases.add(single);
        }
        envelope.set("cases", cases);

        Process process = new ProcessBuilder("python3", "-I", "-B", harness.toString())
                .redirectErrorStream(true)
                .start();
        try (var stdin = process.getOutputStream()) {
            stdin.write(MAPPER.writeValueAsBytes(envelope));
        }
        assertThat(process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS))
                .as("the harness must not hang on a reference solution")
                .isTrue();

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(stdout).as("the harness must always emit an answer").isNotBlank();
        return MAPPER.readTree(stdout);
    }
}