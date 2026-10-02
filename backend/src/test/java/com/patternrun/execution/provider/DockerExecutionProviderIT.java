package com.patternrun.execution.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.patternrun.execution.ExecutionCase;
import com.patternrun.execution.ExecutionOutcome;
import com.patternrun.execution.ExecutionProperties;
import com.patternrun.execution.ExecutionReport;
import com.patternrun.execution.ExecutionRequest;
import com.patternrun.problem.ArgumentMode;
import com.patternrun.problem.ProgrammingLanguage;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The container boundary, against a real container.
 *
 * Every test here is a claim about isolation that would be worthless as documentation if it were
 * not checked. A sandbox that quietly stops holding is the worst possible failure in this feature,
 * because it still looks like it works.
 *
 * <p>Skipped unless the executor image is present, so the suite still passes on a machine without a
 * container runtime. Build it first:
 *
 * <pre>docker build -t patternrun-executor:py3 backend/executor</pre>
 */
@EnabledIf("imageIsAvailable")
class DockerExecutionProviderIT {

    private static final String IMAGE = "patternrun-executor:py3";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DockerExecutionProvider provider;

    @SuppressWarnings("unused") // referenced by @EnabledIf
    static boolean imageIsAvailable() {
        try {
            Process probe = new ProcessBuilder("docker", "image", "inspect", IMAGE)
                    .redirectErrorStream(true)
                    .start();
            return probe.waitFor(20, TimeUnit.SECONDS) && probe.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    @BeforeEach
    void buildProvider() {
        ExecutionProperties properties = new ExecutionProperties();
        properties.setEnabled(true);
        properties.docker().setImage(IMAGE);
        // Generous, because container start is not the thing under test and a loaded machine
        // would otherwise produce spurious timeouts that look like time-limit findings.
        properties.docker().setWallClockTimeout(java.time.Duration.ofSeconds(60));
        properties.docker().setTotalTimeout(java.time.Duration.ofSeconds(3));

        provider = new DockerExecutionProvider(properties, new PythonHarness(), MAPPER);
    }

    private static JsonNode array(String json) {
        return MAPPER.readTree(json);
    }

    private ExecutionReport run(String source, List<ExecutionCase> cases) {
        return run("two_sum", source, cases, 2.0d);
    }

    /**
     * The entrypoint is the server's to choose, so a probe that is not the problem's function has
     * to say which name the runner will call.
     */
    private ExecutionReport run(String entrypoint, String source, List<ExecutionCase> cases, double timeout) {
        return run(entrypoint, source, cases, timeout, ArgumentMode.PLAIN);
    }

    /** The tree problems take a level-order array where a node is expected. */
    private ExecutionReport runTree(String entrypoint, String source, List<ExecutionCase> cases) {
        return run(entrypoint, source, cases, 5.0d, ArgumentMode.TREE);
    }

    private ExecutionReport run(String entrypoint, String source, List<ExecutionCase> cases,
                                double timeout, ArgumentMode mode) {
        return provider.execute(new ExecutionRequest(
                ProgrammingLanguage.PYTHON, source, entrypoint, cases, timeout, mode));
    }

    /**
     * {@code args} is the whole positional argument list, which is what makes a case content-driven
     * rather than specific to one problem's signature.
     */
    private static List<ExecutionCase> twoCases() {
        return List.of(
                new ExecutionCase("visible", array("[[2,7,11,15],9]"), array("[0,1]"), false),
                new ExecutionCase("hidden", array("[[3,2,4],6]"), array("[1,2]"), true));
    }

    private static final String WORKING = """
            def two_sum(nums, target):
                seen = {}
                for index, value in enumerate(nums):
                    if target - value in seen:
                        return [seen[target - value], index]
                    seen[value] = index
                return []
            """;

    // ------------------------------------------------------------- the happy path

    @Test
    @DisplayName("A correct solution is accepted through the real command line")
    void acceptsThroughAContainer() {
        ExecutionReport report = run(WORKING, twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.ACCEPTED);
        assertThat(report.casesPassed()).isEqualTo(2);
        assertThat(report.caseOutcomes()).allSatisfy(outcome ->
                assertThat(outcome.actual()).isNotNull());
    }

    @Test
    @DisplayName("A wrong answer is distinguishable from a crash, end to end")
    void wrongAnswer() {
        ExecutionReport report = run("def two_sum(nums, target): return [9, 9]", twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.WRONG_ANSWER);
        assertThat(report.casesPassed()).isZero();
    }

    @Test
    @DisplayName("A syntax error survives the round trip")
    void syntaxError() {
        ExecutionReport report = run("def two_sum(nums, target)\n    return [0, 1]", twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.SYNTAX_ERROR);
        assertThat(report.errorLine()).isEqualTo(1);
        assertThat(report.errorType()).isEqualTo("SyntaxError");
    }

    @Test
    @DisplayName("A runtime error carries the learner's line through the container")
    void runtimeError() {
        ExecutionReport report = run("def two_sum(nums, target):\n    return target - nums", twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.RUNTIME_ERROR);
        assertThat(report.errorType()).isEqualTo("TypeError");
        assertThat(report.errorLine()).isEqualTo(2);
    }

    @Test
    @DisplayName("A run that overruns is stopped and reported as a timeout")
    void timeout() {
        ExecutionReport report = provider.execute(new ExecutionRequest(
                ProgrammingLanguage.PYTHON, "def two_sum(nums, target):\n while True: pass",
                "two_sum", twoCases(), 1.0d, ArgumentMode.PLAIN));

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("An allocation the container cannot hold is reported as memory, not as a runner fault")
    void memoryLimit() {
        ExecutionReport report = run(
                "def two_sum(nums, target):\n    hog = [0] * 250_000_000\n    return [0, 1]", twoCases());

        // The OOM killer sends 137 and the harness never gets to answer. Reading that as an
        // infrastructure failure would tell a learner their working solution is broken.
        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.MEMORY_LIMIT_EXCEEDED);
    }

    // ------------------------------------------------------- the isolation claims

    @Test
    @DisplayName("There is no network, so nothing can be sent out")
    void noNetwork() {
        ExecutionReport report = run("""
                import urllib.request
                def two_sum(nums, target):
                    urllib.request.urlopen("http://example.com", timeout=3)
                    return [0, 1]
                """, twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.RUNTIME_ERROR);
        assertThat(report.errorMessage().toLowerCase())
                .as("the failure must be a network failure, not a successful request")
                .containsAnyOf("name resolution", "network", "unreachable", "temporary failure");
    }

    @Test
    @DisplayName("A raw socket cannot leave either, so DNS is not the only door")
    void noRawSocket() {
        ExecutionReport report = run("""
                import socket
                def two_sum(nums, target):
                    socket.create_connection(("1.1.1.1", 80), 3)
                    return [0, 1]
                """, twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("The filesystem is read-only outside the scratch area")
    void readOnlyFilesystem() {
        ExecutionReport report = run("""
                def two_sum(nums, target):
                    open("/etc/pwned", "w").write("x")
                    return [0, 1]
                """, twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.RUNTIME_ERROR);
        assertThat(report.errorMessage()).contains("Read-only");
    }

    /**
     * Asserted on what the code returned, not on whether it passed.
     *
     * The uid is whatever the runtime chose, so pinning the verdict would mean asserting a value
     * this test does not know. The claim being made is "the number is not 0", and reading it off
     * the output is the honest way to make it.
     */
    @Test
    @DisplayName("Execution is not root")
    void nonRoot() {
        ExecutionReport report = run("uid_probe", """
                import os
                def uid_probe():
                    return [os.getuid(), 0]
                """, List.of(new ExecutionCase("uid", array("[]"), array("[[0, 0]]"), false)), 2.0d);

        assertThat(report.caseOutcomes()).hasSize(1);
        assertThat(report.caseOutcomes().get(0).actual())
                .as("uid 0 would mean root inside the container")
                .isNotEqualTo("[0, 0]");
        assertThat(report.caseOutcomes().get(0).actual()).matches("\\[\\d+, 0\\]");
    }

    /**
     * The claim that matters most, because it is the one that would be catastrophic and the one
     * that is easiest to lose by accident — a bind mount, or a compose file edited in a hurry.
     */
    @Test
    @DisplayName("The API's environment does not reach the container")
    void noApiEnvironment() {
        ExecutionReport report = run("env_probe", """
                import os, json
                def env_probe():
                    return json.dumps(sorted(os.environ))
                """, List.of(new ExecutionCase("env", array("[]"), array("[]"), false)), 2.0d);

        String environment = report.caseOutcomes().get(0).actual();
        // Nothing this application uses. The database credentials live in exactly these names.
        assertThat(environment).doesNotContain("DB_URL");
        assertThat(environment).doesNotContain("DB_PASSWORD");
        assertThat(environment).doesNotContain("DB_USERNAME");
        assertThat(environment).doesNotContain("SPRING_DATASOURCE");
    }

    @Test
    @DisplayName("No host path is mounted, so there is nothing to go looking for")
    void noHostMounts() {
        ExecutionReport report = run("mount_probe", """
                def mount_probe():
                    mounts = open("/proc/self/mounts").read()
                    for line in mounts.splitlines():
                        if "/Users/" in line or "/home/patternrun" in line or "docker.sock" in line:
                            return [0, 1]
                    return [9, 9]
                """, List.of(new ExecutionCase("mounts", array("[]"), array("[9, 9]"), false)), 2.0d);

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.ACCEPTED);
    }

    @Test
    @DisplayName("A fork loop is stopped rather than being allowed to spread")
    void pidLimit() {
        ExecutionReport report = run("""
                import os
                def two_sum(nums, target):
                    for _ in range(2000):
                        if os.fork() == 0:
                            os._exit(0)
                    return [0, 1]
                """, twoCases());

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.RUNTIME_ERROR);
    }

    // ---------------------------------------------------------- the disabled path

    @Test
    @DisplayName("A disabled provider refuses rather than falling back to running code anywhere else")
    void disabledRefuses() {
        ExecutionProperties properties = new ExecutionProperties();
        properties.setEnabled(false);

        DockerExecutionProvider disabled =
                new DockerExecutionProvider(properties, new PythonHarness(), MAPPER);

        ExecutionReport report = disabled.execute(new ExecutionRequest(
                ProgrammingLanguage.PYTHON, WORKING, "two_sum", twoCases(), 2.0d, ArgumentMode.PLAIN));

        assertThat(disabled.isAvailable()).isFalse();
        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.INTERNAL_ERROR);
        assertThat(report.errorMessage()).contains("not enabled");
    }
}