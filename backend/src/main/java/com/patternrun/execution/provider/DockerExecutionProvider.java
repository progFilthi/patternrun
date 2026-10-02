package com.patternrun.execution.provider;

import com.patternrun.execution.CodeExecutionProvider;
import com.patternrun.execution.ExecutionCase;
import com.patternrun.execution.ExecutionOutcome;
import com.patternrun.execution.ExecutionProperties;
import com.patternrun.execution.ExecutionReport;
import com.patternrun.execution.ExecutionRequest;
import com.patternrun.problem.ArgumentMode;
import com.patternrun.problem.ProgrammingLanguage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs each submission in a throwaway container and reads back a structured answer.
 *
 * <h2>Where the security boundary is</h2>
 *
 * The container. Nothing in this class enforces anything; it assembles a command line and reads a
 * result, and every guarantee below comes from a flag on that command line. Worth stating plainly,
 * because the alternative — a harness that sandboxes by inspecting the code — is subverted by the
 * first learner who did not mean any harm, and no version of that is safe.
 *
 * <h2>What the container guarantees</h2>
 *
 * <ul>
 *   <li>{@code --network=none} — no egress, so nothing can be sent out or called home.</li>
 *   <li>{@code --read-only} with a small {@code --tmpfs} — the image cannot be rewritten, and there
 *       is nowhere on disk to keep anything between runs.</li>
 *   <li>{@code --user=65534:65534} — nobody. Not root, not this process's user, not the image's.</li>
 *   <li>{@code --cap-drop=ALL} and {@code --security-opt=no-new-privileges} — nothing to escalate
 *       from, and no way to gain more.</li>
 *   <li>{@code --memory}, {@code --cpus}, {@code --pids-limit} — a runaway loop is bounded rather
 *       than fatal, and a fork bomb stops at the pid limit.</li>
 *   <li>No environment is forwarded. The API's own variables, including its database credentials,
 *       are not on the command line and are not passed in.</li>
 *   <li>No host path is mounted. Not even the runner itself: the harness arrives over stdin, so the
 *       container has no idea the host has a filesystem it could ask about.</li>
 *   <li>{@code --rm} — gone before the next one starts, so nothing accumulates.</li>
 * </ul>
 *
 * <h2>What this class is not</h2>
 *
 * It is not the boundary, and neither is the harness. The API process never executes submitted
 * code: it runs the docker client, which is a separate process talking to the daemon over its
 * socket. Deleting this class and replacing it with a compiled, remote or kernel-level provider
 * would not weaken anything, because the guarantees live in the container configuration, not here.
 */
@Component
public class DockerExecutionProvider implements CodeExecutionProvider {

    private static final Logger log = LoggerFactory.getLogger(DockerExecutionProvider.class);

    /** nobody:nogroup on the Debian-family base the image uses. */
    private static final String NON_ROOT_USER = "65534:65534";

    /**
     * The bootstrap that runs the harness out of stdin.
     *
     * A bind mount would be the obvious alternative, but stdin means the container is never told a
     * host path exists, so there is nothing for submitted code to go looking for. It also means
     * nothing has to be cleaned up on the host after a run.
     *
     * The bootstrap restores {@code sys.stdin} over the payload half so the harness reads its
     * request through the same {@code sys.stdin.read()} it would use normally.
     */
    private static final String BOOTSTRAP = """
            import sys, io
            _data = sys.stdin.buffer.read()
            _harness, _payload = _data.split(b"\\x00PATTERNRUN\\x00", 1)
            sys.stdin = io.TextIOWrapper(io.BytesIO(_payload), encoding="utf-8")
            exec(compile(_harness, "harness.py", "exec"),
                 {"__name__": "__main__", "__builtins__": __builtins__})
            """;

    /**
     * Output larger than this is a runaway rather than a result.
     *
     * The harness bounds what the learner's code prints, so this only guards against the harness
     * itself misbehaving. It exists because an unbounded read of a child process is a way to
     * exhaust this API's heap, which is an attack on the server rather than on the submission.
     */
    private static final int MAX_HARNESS_OUTPUT_BYTES = 4 * 1024 * 1024;

    private static final int STDERR_CAPTURE_BYTES = 8 * 1024;

    /** 128 + 9. The OOM killer's exit code, which is what a memory cap produces. */
    private static final int EXIT_OOM_KILLED = 137;

    /**
     * The runner's word for "it ran to the end".
     *
     * Not an {@link ExecutionOutcome}, deliberately. It carries no verdict, and mapping it to one
     * is {@code outcomeFor}'s job.
     */
    private static final String RUNNER_COMPLETED = "COMPLETED";

    private final ExecutionProperties properties;
    private final PythonHarness harness;
    private final ObjectMapper objectMapper;
    private final Semaphore concurrency;

    public DockerExecutionProvider(ExecutionProperties properties,
                                    PythonHarness harness,
                                    ObjectMapper objectMapper) {
        this.properties = properties;
        this.harness = harness;
        this.objectMapper = objectMapper;
        this.concurrency = new Semaphore(Math.max(1, properties.docker().getMaxConcurrent()));
    }

    @Override
    public ProgrammingLanguage language() {
        return ProgrammingLanguage.PYTHON;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled();
    }

    @Override
    public ExecutionReport execute(ExecutionRequest request) {
        if (!isAvailable()) {
            return ExecutionReport.infrastructureFailure("Running code is not enabled on this server.");
        }
        if (!concurrency.tryAcquire()) {
            return ExecutionReport.infrastructureFailure("The runner is busy. Try again in a moment.");
        }
        try {
            return run(request);
        } finally {
            concurrency.release();
        }
    }

    private ExecutionReport run(ExecutionRequest request) {
        long startedAt = System.nanoTime();
        byte[] payload = encode(request);
        if (payload == null) {
            return ExecutionReport.infrastructureFailure("The submission could not be encoded.");
        }

        Process process = null;
        try {
            process = new ProcessBuilder(command()).start();

            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(payload);
                stdin.flush();
            }

            // Both pipes are drained on their own threads before waiting. Reading them after
            // waitFor would deadlock the moment a child filled a pipe buffer, which a learner's
            // print() loop does easily.
            StreamReader stdout = StreamReader.start(process.getInputStream(), MAX_HARNESS_OUTPUT_BYTES);
            StreamReader stderr = StreamReader.start(process.getErrorStream(), STDERR_CAPTURE_BYTES);

            long ceiling = properties.docker().getWallClockTimeout().toMillis();
            if (!process.waitFor(ceiling, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                stdout.join();
                stderr.join();
                log.warn("Submission hit the wall-clock ceiling of {}ms and was killed", ceiling);
                return new ExecutionReport(ExecutionOutcome.INTERNAL_ERROR, List.of(),
                        elapsedMs(startedAt), "RunnerTimeout",
                        "The runner did not come back in time.", null, "", "");
            }

            stdout.join();
            stderr.join();
            return interpret(process.exitValue(), stdout.text(), stderr.text(), elapsedMs(startedAt));
        } catch (IOException ex) {
            log.warn("Could not start the execution container: {}", ex.toString());
            return ExecutionReport.infrastructureFailure("The runner could not be started.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ExecutionReport.infrastructureFailure("The runner was interrupted.");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * The command line. Every flag here is a security control and is commented as one.
     *
     * Read it as a whole before changing it. Dropping {@code --network=none} or {@code --read-only}
     * does not degrade this feature, it converts it from "runs a learner's program in a box" to
     * "runs a learner's program".
     */
    private List<String> command() {
        ExecutionProperties.Docker docker = properties.docker();
        List<String> command = new ArrayList<>(32);

        command.add(docker.getBinary());
        command.add("run");
        command.add("--rm");

        // --- Isolation. Removing any of these breaks the boundary. ---
        command.add("--network=none");
        command.add("--read-only");
        command.add("--user=" + NON_ROOT_USER);
        command.add("--cap-drop=ALL");
        command.add("--security-opt=no-new-privileges");
        command.add("--pids-limit=" + docker.getPids());
        command.add("--memory=" + docker.getMemory());
        // Equal swap means the memory limit cannot be side-stepped by swapping.
        command.add("--memory-swap=" + docker.getMemory());
        command.add("--cpus=" + docker.getCpus());
        // The one writable path, noexec so nothing in it can be run, and small enough to fill.
        command.add("--tmpfs=/tmp:rw,noexec,nosuid,nodev,size=" + docker.getTmpfsSize());
        command.add("--workdir=/tmp");
        // No -e flags, and no bind mounts. Nothing of this API's reaches the container except the
        // learner's own source, on stdin.
        command.add("-i");
        command.add(docker.getImage());

        // -I isolates the interpreter from the environment, the user site directory and PYTHONPATH,
        // which matters precisely because a learner's code can set those for itself.
        // -B stops it writing .pyc files, which the read-only filesystem would turn into an error.
        command.add("python3");
        command.add("-I");
        command.add("-B");
        command.add("-c");
        command.add(BOOTSTRAP);

        return command;
    }

    /**
     * The request, with the harness in front of it.
     *
     * The NUL separator cannot occur in the harness source, because the harness is a text file, and
     * the split happens on the first occurrence so a payload cannot smuggle a second one in.
     */
    private byte[] encode(ExecutionRequest request) {
        try {
            String body = objectMapper.writeValueAsString(new HarnessPayload(
                    request.source(),
                    request.entrypoint(),
                    request.cases().stream().map(HarnessCase::from).toList(),
                    request.timeoutSeconds(),
                    request.argumentMode()));
            return (harness.source() + "\0PATTERNRUN\0" + body).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            log.warn("Could not encode a submission for the runner", ex);
            return null;
        }
    }

    /**
     * Turns the harness's answer into a report.
     *
     * <h3>The runner's status is not the verdict</h3>
     *
     * The harness reports {@code COMPLETED} for anything that ran to the end, whether or not the
     * answers were right, and this method is where that becomes {@link ExecutionOutcome#ACCEPTED} or
     * {@link ExecutionOutcome#WRONG_ANSWER}. Passing the runner's word straight through was a real
     * bug: naming its success status {@code ACCEPTED} meant every failing submission was reported
     * as accepted. The runner knows how to call a function; it does not know what a correct answer
     * is, and that judgement belongs here where the expected values are in scope.
     *
     * <p>An unreadable answer is {@link ExecutionOutcome#INTERNAL_ERROR}, never
     * {@code WRONG_ANSWER}. Reporting a broken runner as a wrong answer is the worst thing this
     * feature could do, because it tells a learner their working solution is broken.
     */
    private ExecutionReport interpret(int exitCode, String stdout, String stderr, Long durationMs) {
        String trimmed = stdout == null ? "" : stdout.trim();
        if (trimmed.isEmpty()) {
            // A container killed from outside has no JSON to give. Exit 137 is SIGKILL, which for a
            // container with a memory cap means the learner allocated too much, not that our runner
            // broke. Attributing it to the runner would be wrong in the one case where the learner
            // can act on it, and INTERNAL_ERROR would tell them their code is fine.
            if (exitCode == EXIT_OOM_KILLED) {
                return new ExecutionReport(ExecutionOutcome.MEMORY_LIMIT_EXCEEDED, List.of(), durationMs,
                        "MemoryError", "The solution used more memory than the runner allows.", null, "", stderr);
            }
            log.warn("Harness produced no output (exit {}). stderr: {}", exitCode, stderr);
            return new ExecutionReport(ExecutionOutcome.INTERNAL_ERROR, List.of(), durationMs,
                    "RunnerUnavailable", "The runner produced no result.", null, "", stderr);
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            JsonNode error = node.path("error");
            JsonNode errorLine = error.path("line");
            List<ExecutionReport.CaseOutcome> cases = readCases(node.path("cases"), node);

            return new ExecutionReport(
                    outcomeFor(node.path("status").asText("INTERNAL_ERROR"), cases),
                    cases,
                    durationMs,
                    emptyToNull(error.path("type").asText(null)),
                    emptyToNull(error.path("message").asText(null)),
                    errorLine.isInt() ? errorLine.asInt() : null,
                    "",
                    stderr);
        } catch (RuntimeException ex) {
            log.warn("Harness output was unreadable (exit {}): {}", exitCode, trimmed);
            return new ExecutionReport(ExecutionOutcome.INTERNAL_ERROR, List.of(), durationMs,
                    "RunnerUnavailable", "The runner produced an unreadable result.", null,
                    trimmed, stderr);
        }
    }

    /**
     * The one place a verdict is derived from case results.
     *
     * {@code COMPLETED} means the code ran, so the answer is the cases. Zero cases is not a pass:
     * nothing was tested, and reporting "accepted" for an empty run would be a vacuous truth the
     * learner has no way to interpret.
     */
    private static ExecutionOutcome outcomeFor(String status, List<ExecutionReport.CaseOutcome> cases) {
        if (!RUNNER_COMPLETED.equals(status)) {
            return parseOutcome(status);
        }
        if (cases.isEmpty()) {
            log.warn("The runner completed without evaluating any case");
            return ExecutionOutcome.INTERNAL_ERROR;
        }
        return cases.stream().allMatch(ExecutionReport.CaseOutcome::passed)
                ? ExecutionOutcome.ACCEPTED
                : ExecutionOutcome.WRONG_ANSWER;
    }

    /**
     * Reads the positional case results.
     *
     * Per-case rather than derived from a total, because a total cannot say which case failed. With
     * cases [pass, fail, pass] the count is two, and anything reconstructed from that would tell the
     * learner their third case failed when it passed.
     */
    private static List<ExecutionReport.CaseOutcome> readCases(JsonNode cases, JsonNode node) {
        String runStatus = node.path("status").asText("INTERNAL_ERROR");
        List<ExecutionReport.CaseOutcome> results = new ArrayList<>(cases.size());
        for (JsonNode entry : cases) {
            boolean passed = entry.path("passed").asBoolean(false);
            JsonNode actual = entry.path("actual");
            String rendered = actual.isMissingNode() || actual.isNull() ? null : actual.asText(null);

            if (passed) {
                results.add(ExecutionReport.CaseOutcome.passed(rendered));
            } else if (entry.hasNonNull("error")) {
                // A case that raised is not merely wrong. Reporting it as a wrong answer would
                // explain the failure in terms the learner can do nothing with.
                results.add(ExecutionReport.CaseOutcome.threw(parseOutcome(
                        entry.path("error").path("status").asText(runStatus))));
            } else {
                results.add(ExecutionReport.CaseOutcome.wrong(rendered));
            }
        }
        return List.copyOf(results);
    }

        /**
     * Reads one of the harness's statuses.
 *
 * Falls back to {@link ExecutionOutcome#INTERNAL_ERROR} rather than throwing, because an
 * unrecognised status must not take the endpoint down. It is logged, though, and loudly: the two
 * vocabularies are supposed to be identical, so a mismatch means one of them was renamed without
 * the other. This exact drift happened once — the harness said {@code "OK"} and the enum had no
 * such member — and every passing submission was reported as an infrastructure failure while the
 * learner's code was fine.
 */
    private static ExecutionOutcome parseOutcome(String status) {
        try {
            return ExecutionOutcome.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.warn("The runner reported an outcome this server does not know: \"{}\"", status);
            return ExecutionOutcome.INTERNAL_ERROR;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    /** The exact shape the harness reads, named field for field. */
    private record HarnessPayload(
            String source,
            String entrypoint,
            List<HarnessCase> cases,
            double timeoutSeconds,
            ArgumentMode argumentMode) {
    }

    private record HarnessCase(String label, JsonNode args, JsonNode expected) {
        static HarnessCase from(ExecutionCase testCase) {
            return new HarnessCase(testCase.label(), testCase.args(), testCase.expected());
        }
    }

    /**
     * Drains a child stream on its own thread, up to a cap.
     *
     * The thread is joined before the result is read, so {@link #text()} is only observed after the
     * stream is finished. Truncation is not silent here: the caller sees a short string and turns it
     * into a runner failure rather than acting on a partial answer.
     */
    private static final class StreamReader {

        private Thread thread;
        private volatile String text = "";

        private StreamReader() {
        }

        static StreamReader start(InputStream stream, int limit) {
            StreamReader reader = new StreamReader();
            // The worker writes to this same instance. An earlier version captured a second,
            // throwaway StreamReader for the lambda and every read came back empty — which the
            // tests caught as "the harness produced no output" rather than as a null dereference.
            reader.thread = new Thread(() -> {
                ByteArrayOutputStream sink = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                try {
                    int read;
                    while (sink.size() < limit && (read = stream.read(buffer)) != -1) {
                        sink.write(buffer, 0, Math.min(read, limit - sink.size()));
                    }
                } catch (IOException ignored) {
                    // Partial output. The caller treats a truncated result as a runner failure.
                } finally {
                    reader.text = sink.toString(StandardCharsets.UTF_8);
                }
            }, "patternrun-exec-output");
            reader.thread.setDaemon(true);
            reader.thread.start();
            return reader;
        }

        void join() throws InterruptedException {
            thread.join();
        }

        String text() {
            return text;
        }
    }
}