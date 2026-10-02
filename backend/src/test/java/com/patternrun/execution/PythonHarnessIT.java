package com.patternrun.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The Python runner, exercised against a real interpreter.
 *
 * No container, because what is under test is the translation layer: that a wrong answer is
 * distinguished from a crash, that a crash carries the learner's line rather than a path, that an
 * infinite loop becomes a reportable timeout rather than a killed process, and that a stray
 * {@code print()} cannot corrupt the answer. Those are properties of the harness and hold whichever
 * sandbox contains it.
 *
 * {@link DockerExecutionProviderIT} covers the container boundary itself.
 */
@EnabledIf("pythonIsAvailable")
class PythonHarnessIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path workingDirectory;

    private Path harness;

    @SuppressWarnings("unused") // referenced by @EnabledIf
    static boolean pythonIsAvailable() {
        try {
            Process probe = new ProcessBuilder("python3", "--version")
                    .redirectErrorStream(true)
                    .start();
            return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @BeforeEach
    void copyHarness() throws IOException {
        // Run the shipped file rather than a copy of it, so this test breaks if the resource is
        // missing instead of quietly passing against something else.
        try (var stream = getClass().getResourceAsStream("/code-execution/python-harness.py")) {
            assertThat(stream).as("the harness must be on the classpath").isNotNull();
            harness = workingDirectory.resolve("harness.py");
            java.nio.file.Files.write(harness, stream.readAllBytes());
        }
    }

    private static final String WORKING = """
            def two_sum(nums, target):
                seen = {}
                for index, value in enumerate(nums):
                    needed = target - value
                    if needed in seen:
                        return [seen[needed], index]
                    seen[value] = index
                return []
            """;

    /**
     * Runs the harness as the plain script it is: one JSON request on stdin, one JSON answer on
     * stdout.
     *
     * The provider's transport framing (source, marker and request concatenated) is its own
     * concern and is covered by {@code DockerExecutionProviderIT}. Handing that framing to a direct
     * invocation would test the transport twice and the harness zero times.
     */
    private JsonNode run(String source, JsonNode request, double timeoutSeconds) throws Exception {
        // Built without chaining: Jackson 3's ObjectNode.set returns the node it assigned rather
        // than the parent, so a chained call serialises only the last assignment.
        var envelope = MAPPER.createObjectNode();
        envelope.put("source", source);
        envelope.put("entrypoint", "two_sum");
        envelope.put("timeoutSeconds", timeoutSeconds);
        envelope.set("cases", request.path("cases"));

        String payload = MAPPER.writeValueAsString(envelope);

        Process process = new ProcessBuilder(
                "python3", "-I", "-B", harness.toString())
                .directory(workingDirectory.toFile())
                .redirectErrorStream(false)
                .start();

        try (var stdin = process.getOutputStream()) {
            stdin.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("the harness must not hang").isTrue();

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(stdout).as("the harness must always emit a result").isNotBlank();
        return MAPPER.readTree(stdout);
    }

    private JsonNode runWithCases(String source, List<String> cases, double timeoutSeconds) throws Exception {
        var array = MAPPER.createArrayNode();
        for (String definition : cases) {
            array.add(MAPPER.readTree(definition));
        }
        var request = MAPPER.createObjectNode();
        request.set("cases", array);
        return run(source, request, timeoutSeconds);
    }

    private static final String C1 = "{\"label\":\"one\",\"args\":[[2,7,11,15],9],\"expected\":[0,1]}";
    private static final String C2 = "{\"label\":\"dupes\",\"args\":[[3,2,4],6],\"expected\":[1,2]}";
    private static final String C3 = "{\"label\":\"same element twice\",\"args\":[[3,3],6],\"expected\":[0,1]}";

    // ------------------------------------------------------------------ outcomes

    @Test
    @DisplayName("A correct solution passes every case")
    void acceptsACorrectSolution() throws Exception {
        JsonNode result = runWithCases(WORKING, List.of(C1, C2, C3), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("cases")).hasSize(3);
        assertThat(result.path("cases").get(0).path("passed").asBoolean()).isTrue();
        assertThat(result.path("cases").get(2).path("passed").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Returning the same indices in the other order is the same answer")
    void orderDoesNotMatterForTheIndices() throws Exception {
        String reversed = """
                def two_sum(nums, target):
                    seen = {}
                    for index, value in enumerate(nums):
                        if target - value in seen:
                            return [index, seen[target - value]]
                        seen[value] = index
                    return []
                """;

        JsonNode result = runWithCases(reversed, List.of(C1, C2), 2.0);

        // Telling someone they were wrong for writing [1, 0] would be teaching something false.
        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Storing the current value before testing for it fails the self-pair case")
    void catchesStoreBeforeCheck() throws Exception {
        String buggy = """
                def two_sum(nums, target):
                    seen = {}
                    for index, value in enumerate(nums):
                        seen[value] = index
                        if target - value in seen:
                            return [seen[target - value], index]
                    return []
                """;

        JsonNode result = runWithCases(buggy, List.of(C3), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("cases").get(0).path("passed").asBoolean())
                .as("nums = [3,3] with target 6 must not let one element match itself")
                .isFalse();
    }

    @Test
    @DisplayName("A wrong return is WRONG_ANSWER, and carries what came back")
    void wrongAnswerIsNotAnError() throws Exception {
        JsonNode result = runWithCases("def two_sum(nums, target): return [9, 9]", List.of(C1), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("cases").get(0).path("passed").asBoolean()).isFalse();
        assertThat(result.path("cases").get(0).path("actual").asText()).contains("9");
        assertThat(result.path("error").isNull()).isTrue();
    }

    @Test
    @DisplayName("A syntax error names the line, and says the file is the learner's")
    void syntaxErrorCarriesTheLine() throws Exception {
        JsonNode result = runWithCases("def two_sum(nums, target)\n    return [0, 1]", List.of(C1), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("SYNTAX_ERROR");
        assertThat(result.path("error").path("type").asText()).isEqualTo("SyntaxError");
        assertThat(result.path("error").path("line").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("An unexpected indent is reported where Python reports it, not where it was caused")
    void indentationErrorCarriesTheLine() throws Exception {
        JsonNode result = runWithCases(
                "def two_sum(nums, target):\n  return [0, 1]\n    return [1, 0]", List.of(C1), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("SYNTAX_ERROR");
        assertThat(result.path("error").path("type").asText()).isEqualTo("IndentationError");
        assertThat(result.path("error").path("line").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("A crash is RUNTIME_ERROR with the exception type and the learner's own line")
    void runtimeErrorCarriesTheLine() throws Exception {
        JsonNode result = runWithCases(
                "def two_sum(nums, target):\n    return target - nums", List.of(C1), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("RUNTIME_ERROR");
        assertThat(result.path("error").path("type").asText()).isEqualTo("TypeError");
        assertThat(result.path("error").path("line").asInt())
                .as("the line must be in the learner's file, not the harness's")
                .isEqualTo(2);
        assertThat(result.path("error").path("message").asText()).contains("unsupported operand");
    }

    @Test
    @DisplayName("A missing entrypoint is explained in terms of what the runner calls")
    void missingEntrypointIsExplained() throws Exception {
        JsonNode result = runWithCases("def wrong_name(nums, target): return [0, 1]", List.of(C1), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("RUNTIME_ERROR");
        assertThat(result.path("error").path("type").asText()).isEqualTo("NameError");
        assertThat(result.path("error").path("message").asText()).contains("two_sum");
    }

    @Test
    @DisplayName("No path from this machine ever reaches the message")
    void neverLeaksAPath() throws Exception {
        JsonNode result = runWithCases(
                "def two_sum(nums, target):\n    raise ValueError(open('/etc/passwd').read()[:40])",
                List.of(C1), 2.0);

        String message = result.path("error").path("message").asText();
        assertThat(result.path("status").asText()).isEqualTo("RUNTIME_ERROR");
        // The learner's own text is theirs to see; the harness's file layout is not.
        assertThat(message).doesNotContain("python-harness.py");
        assertThat(message).doesNotContain(workingDirectory.toString());
    }

    // ------------------------------------------------------------------ timeouts

    @Test
    @DisplayName("An infinite loop becomes a timeout rather than a killed process")
    void infiniteLoopTimesOut() throws Exception {
        JsonNode result = runWithCases(
                "def two_sum(nums, target):\n    while True:\n        pass", List.of(C1), 1.0);

        // The default disposition of SIGALRM terminates the process, so without a handler this
        // would produce no output at all and the learner would be told nothing.
        assertThat(result.path("status").asText()).isEqualTo("TIME_LIMIT_EXCEEDED");
        assertThat(result.path("error").path("message").asText()).contains("in time");
    }

    @Test
    @DisplayName("A brute force solution is too slow on a large input, and is told so")
    void quadraticSolutionTimesOut() throws Exception {
        // args is the whole positional argument list: the array and the target, not a flat array.
        // 8000 elements is 64M inner iterations, which is comfortably past one second on any
        // machine rather than marginal, so the assertion cannot pass or fail on how fast it is.
        String huge = "{\"label\":\"big\",\"args\":[[" + "0,".repeat(8000) + "0],6],\"expected\":[0,1]}";
        String bruteForce = """
                def two_sum(nums, target):
                    for i in range(len(nums)):
                        for j in range(len(nums)):
                            if nums[i] + nums[j] == target:
                                return [i, j]
                    return []
                """;

        JsonNode result = runWithCases(bruteForce, List.of(huge), 1.0);

        // This is the case that teaches complexity, and it only works if the limit is real.
        assertThat(result.path("status").asText()).isEqualTo("TIME_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Unbounded recursion is reported, not left to take the process down")
    void recursionIsReported() throws Exception {
        JsonNode result = runWithCases("def two_sum(nums, target): return two_sum(nums, target)",
                List.of(C1), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("RUNTIME_ERROR");
        assertThat(result.path("error").path("type").asText()).isEqualTo("RecursionError");
    }

    // -------------------------------------------------------------- stray output

    @Test
    @DisplayName("A stray print does not corrupt the answer")
    void strayPrintIsHarmless() throws Exception {
        String chatty = WORKING.replace("    seen = {}",
                "    print('about to scan', nums)\n    seen = {}");

        JsonNode result = runWithCases(chatty, List.of(C1, C2), 2.0);

        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("cases")).allSatisfy(
                entry -> assertThat(entry.path("passed").asBoolean()).isTrue());
    }

    @Test
    @DisplayName("A print loop is stopped by the time limit instead of exhausting memory")
    void printLoopIsBounded() throws Exception {
        String noisy = WORKING.replace("    seen = {}",
                "    for _ in range(10 ** 9):\n        print('x' * 100)\n    seen = {}");

        JsonNode result = runWithCases(noisy, List.of(C1), 1.0);

        assertThat(result.path("status").asText()).isEqualTo("TIME_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("A bounded print loop still completes, so debugging does not break a solution")
    void moderatePrintLoopStillSolves() throws Exception {
        String noisy = WORKING.replace("    seen = {}",
                "    for _ in range(20000):\n        print('debugging')\n    seen = {}");

        JsonNode result = runWithCases(noisy, List.of(C1, C2), 5.0);

        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
    }

    // ------------------------------------------------------------ value rendering

    @Test
    @DisplayName("A value that cannot be serialised is still shown as something readable")
    void unserialisableValueIsRendered() throws Exception {
        JsonNode result = runWithCases("def two_sum(nums, target): return {'a': {1, 2}}",
                List.of(C1), 2.0);

        // Not a crash. A learner returning a set has made a mistake worth naming, not one worth
        // losing their work over.
        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("cases").get(0).path("passed").asBoolean()).isFalse();
        assertThat(result.path("cases").get(0).path("actual")).isNotNull();
    }

    @Test
    @DisplayName("A bool does not pass for the int it happens to equal")
    void boolIsNotAnInt() throws Exception {
        JsonNode result = runWithCases("def two_sum(nums, target): return [True, 1]",
                List.of(C1), 2.0);

        // True == 1 in Python, so a naive comparison would let this through.
        assertThat(result.path("cases").get(0).path("passed").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("A returned value is capped, so a runaway repr cannot flood the result panel")
    void longValueIsCapped() throws Exception {
        JsonNode result = runWithCases("def two_sum(nums, target): return list(range(50000))",
                List.of(C1), 2.0);

        assertThat(result.path("cases").get(0).path("actual").asText()).endsWith("...");
        assertThat(result.path("cases").get(0).path("actual").asText().length()).isLessThan(2100);
    }
}