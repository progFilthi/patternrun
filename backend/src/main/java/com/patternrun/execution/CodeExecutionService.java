package com.patternrun.execution;

import com.patternrun.account.UserEntity;
import com.patternrun.attempt.AttemptEntity;
import com.patternrun.attempt.AttemptRepository;
import com.patternrun.attempt.AttemptService;
import com.patternrun.execution.dto.ExecutionResponse;
import com.patternrun.execution.dto.ExecuteCodeRequest;
import com.patternrun.problem.HintStage;
import com.patternrun.problem.HintTrigger;
import com.patternrun.problem.ProblemEntity;
import com.patternrun.problem.ProblemService;
import com.patternrun.problem.ProblemTestCaseEntity;
import com.patternrun.problem.ProblemTestCaseRepository;
import com.patternrun.problem.ProgrammingLanguage;
import com.patternrun.problem.dto.HintSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * The coding stage of a training session.
 *
 * <h2>Run and Submit are different acts</h2>
 *
 * {@link #run} executes against the visible examples so the learner can check their work. It cannot
 * make a problem solved, and it writes no completion state whatever the outcome — a Run is thinking
 * out loud, and letting a passing Run mark a problem solved would make "check my work" and "I am
 * done" the same button. {@link #submit} executes against the backend-controlled evaluation set,
 * hidden cases included, and only an {@link ExecutionOutcome#ACCEPTED} there writes
 * {@code codeAccepted}.
 *
 * <h2>Why the outcome is decided here</h2>
 *
 * The provider reports what happened; this service decides what it means. That split is what lets
 * the provider stay small enough to be honest about, and what makes replacing it with a remote
 * judge later a change to one class.
 */
@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);

    /** Longest label, input or value echoed back into a result panel. */
    private static final int MAX_DISPLAY_CHARS = 500;

    private final CodeExecutionRepository executions;
    private final AttemptRepository attempts;
    private final ProblemTestCaseRepository testCases;
    private final ProblemService problems;
    private final ExecutionProperties properties;
    private final List<CodeExecutionProvider> providers;

    public CodeExecutionService(CodeExecutionRepository executions,
                                AttemptRepository attempts,
                                ProblemTestCaseRepository testCases,
                                ProblemService problems,
                                ExecutionProperties properties,
                                List<CodeExecutionProvider> availableProviders) {
        this.executions = executions;
        this.attempts = attempts;
        this.testCases = testCases;
        this.problems = problems;
        this.properties = properties;
        // Held as a list and resolved per request rather than indexed by language at construction.
        // Building an index here meant calling language() during context startup, which couples
        // bean creation to every provider answering correctly at that instant. Resolution is a
        // loop over one element.
        this.providers = List.copyOf(availableProviders);
    }

    /** The provider for a language, or null when there is none. */
    private CodeExecutionProvider providerFor(ProgrammingLanguage language) {
        for (CodeExecutionProvider provider : providers) {
            if (provider.language() == language) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Tests the learner's current code against the examples they were already shown.
     *
     * Records the execution, and increments the run counter, because both are history rather than
     * judgement. It never writes {@code codeAccepted} or {@code codeOutcome}: a Run is thinking out
     * loud, and letting a passing Run mark a problem solved would make "check my work" and "I am
     * done" the same button. Nothing a Run writes can complete anything.
     */
    @Transactional
    public ExecutionResponse run(UserEntity user, UUID attemptId, ExecuteCodeRequest request) {
        AttemptEntity attempt = attempts.requireLive(user.getId(), attemptId);
        ProblemEntity problem = requireRunnable(attempt.getProblem());

        List<ProblemTestCaseEntity> cases = testCases.findRunnableVisibleByProblemId(problem.getId());
        ExecutionReport report = execute(user, attempt, problem, request, cases, ExecutionKind.RUN);

        attempt.setCodeExecutions(attempt.getCodeExecutions() + 1);
        attempts.save(attempt);

        return respond(problem, ExecutionKind.RUN, cases, report);
    }

    /**
     * Evaluates against the backend-controlled case set, hidden cases included.
     *
     * The only path that writes {@code codeAccepted}, because it is the only path that runs the
     * hidden cases. That is the whole mechanism behind "only the backend decides when a problem is
     * solved": there is no other writer of that column, and nothing a browser can send reaches this
     * method as a claim.
     */
    @Transactional
    public ExecutionResponse submit(UserEntity user, UUID attemptId, ExecuteCodeRequest request) {
        AttemptEntity attempt = attempts.requireLive(user.getId(), attemptId);
        ProblemEntity problem = requireRunnable(attempt.getProblem());

        List<ProblemTestCaseEntity> cases = testCases.findRunnableByProblemId(problem.getId());
        ExecutionReport report = execute(user, attempt, problem, request, cases, ExecutionKind.SUBMIT);

        attempt.setCodeOutcome(report.outcome());
        attempt.setLanguage(request.languageOrThrow());
        attempt.setCode(request.code());
        // Monotonic, like the XP ledger: once the evaluation set has said yes, a later mistake does
        // not un-solve a problem that was already solved.
        if (report.outcome().isAccepted()) {
            attempt.setCodeAccepted(true);
        }
        attempt.setCodeExecutions(attempt.getCodeExecutions() + 1);
        attempts.save(attempt);

        return respond(problem, ExecutionKind.SUBMIT, cases, report);
    }

    /**
     * Records code without running it.
     *
     * Autosave. It exists so a refresh does not cost a learner their work, and it writes no outcome
     * and no correctness, because nothing has been decided yet.
     */
    @Transactional
    public AttemptCodeState save(UserEntity user, UUID attemptId, ExecuteCodeRequest request) {
        AttemptEntity attempt = attempts.requireLive(user.getId(), attemptId);
        attempt.setLanguage(request.languageOrThrow());
        attempt.setCode(request.code());
        attempts.save(attempt);
        return AttemptCodeState.of(attempt);
    }

    /** The saved source and whatever the last evaluation concluded. Survives a refresh. */
    @Transactional(readOnly = true)
    public AttemptCodeState stateOf(UserEntity user, UUID attemptId) {
        return AttemptCodeState.of(attempts.requireOwned(user.getId(), attemptId));
    }

    /**
     * The next hint for where the learner actually is.
     *
     * The trigger comes from the last execution the <em>server</em> observed. A client asking for a
     * "wrong answer hint" after a passing submission would otherwise get the debugging ladder,
     * which is both the wrong help and a way to walk into the answer without ever having failed.
     */
    @Transactional(readOnly = true)
    public HintSelection nextHint(UserEntity user, UUID attemptId, HintStage stage) {
        HintSelection selection = problems.findHintFor(
                attemptProblemId(user, attemptId), stage, currentTrigger(user, attemptId));
        if (selection == null) {
            // Nothing is staged for this exact moment. The generic ladder still beats silence.
            selection = problems.findHintFor(attemptProblemId(user, attemptId), stage, HintTrigger.ANY);
        }
        return selection;
    }

    /**
     * Which moment the learner is in, as far as the server can tell.
     *
     * The last <em>evaluation</em>, not the last request. Someone who submitted, got it wrong, and
     * then ran the same code to narrow it down has not become less stuck by watching a Run pass;
     * they are mid-diagnosis, which is exactly when a debugging hint is wanted.
     */
    @Transactional(readOnly = true)
    public HintTrigger currentTrigger(UserEntity user, UUID attemptId) {
        return executions
                .findFirstByAttemptIdAndKindOrderByCreatedAtDesc(attemptId, ExecutionKind.SUBMIT)
                .or(() -> executions.findFirstByAttemptIdOrderByCreatedAtDesc(attemptId))
                .map(CodeExecutionEntity::getOutcome)
                .map(CodeExecutionService::triggerFor)
                .orElse(HintTrigger.ANY);
    }

    private UUID attemptProblemId(UserEntity user, UUID attemptId) {
        return attempts.requireOwned(user.getId(), attemptId).getProblem().getId();
    }

    private static HintTrigger triggerFor(ExecutionOutcome outcome) {
        return switch (outcome) {
            case WRONG_ANSWER -> HintTrigger.WRONG_ANSWER;
            case RUNTIME_ERROR -> HintTrigger.RUNTIME_ERROR;
            case SYNTAX_ERROR -> HintTrigger.SYNTAX_ERROR;
            // A timeout is a complexity problem, so the complexity ladder is the right one to
            // offer. Telling someone to check their index arithmetic after a timeout teaches them
            // that the tool does not understand what happened.
            case TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED -> HintTrigger.TIME_LIMIT_EXCEEDED;
            case ACCEPTED, INTERNAL_ERROR -> HintTrigger.ANY;
        };
    }

    private ExecutionReport execute(UserEntity user,
                                    AttemptEntity attempt,
                                    ProblemEntity problem,
                                    ExecuteCodeRequest request,
                                    List<ProblemTestCaseEntity> cases,
                                    ExecutionKind kind) {
        CodeExecutionProvider provider = providerFor(request.languageOrThrow());
        if (provider == null || !provider.isAvailable()) {
            // Reported as our failure, not the learner's. "Running code is unavailable" is honest;
            // anything else would cost someone their belief in a solution that works.
            return ExecutionReport.infrastructureFailure("Running code is not enabled on this server.");
        }

        List<ExecutionCase> runnable = cases.stream()
                .map(testCase -> new ExecutionCase(
                        testCase.getLabel(),
                        testCase.getCall(),
                        testCase.getExpectedJson(),
                        Boolean.TRUE.equals(testCase.getIsHidden())))
                .toList();

        ExecutionReport report = provider.execute(new ExecutionRequest(
                request.languageOrThrow(),
                request.code(),
                problem.getEntrypoint(),
                runnable,
                properties.docker().getTotalTimeout().toMillis() / 1000.0d,
                problem.getArgumentMode()));

        executions.save(record(user, attempt, problem, request, kind, report));
        return report;
    }

    /**
     * Builds the response, and this is the only place hidden cases become something a browser sees.
     *
     * A hidden case contributes its {@code passed} boolean and nothing else: label, input, expected
     * value and actual value are all null. "All hidden cases passed" is the whole useful signal; a
     * label like "Two zeros" would hand over the evaluation set.
     *
     * Per-case results are positional, taken from the report rather than reconstructed from its
     * total. A count cannot say which case failed, and guessing would tell a learner their passing
     * case had failed.
     */
    private ExecutionResponse respond(ProblemEntity problem,
                                      ExecutionKind kind,
                                      List<ProblemTestCaseEntity> cases,
                                      ExecutionReport report) {
        // Nothing was evaluated, so there is nothing per-case to describe.
        //
        // Reporting the cases anyway would render every one as a failure, because a case with no
        // result cannot have passed. The header would then read "that is a problem on our side,
        // not with your code" directly above a list saying the code raised on every example, which
        // is a self-contradiction and worse than saying nothing: a learner reads the detail, not
        // the banner, and concludes their code is broken.
        if (report.caseOutcomes().isEmpty()) {
            return new ExecutionResponse(
                    problem.getSlug(),
                    kind.name(),
                    report.outcome().name(),
                    0,
                    0,
                    report.durationMs(),
                    List.of(),
                    errorDetail(report));
        }

        List<ExecutionResponse.CaseResult> results = new ArrayList<>(cases.size());
        for (int index = 0; index < cases.size(); index++) {
            ProblemTestCaseEntity testCase = cases.get(index);
            ExecutionReport.CaseOutcome outcome = index < report.caseOutcomes().size()
                    ? report.caseOutcomes().get(index)
                    : null;
            boolean passed = outcome != null && outcome.passed();

            results.add(Boolean.TRUE.equals(testCase.getIsHidden())
                    ? new ExecutionResponse.CaseResult(testCase.getOrdinal(), null, null, null, null, passed, true)
                    : new ExecutionResponse.CaseResult(testCase.getOrdinal(), testCase.getLabel(),
                            testCase.getInputData(), testCase.getExpectedOutput(),
                            clamp(outcome == null ? null : outcome.actual()),
                            passed, false));
        }
        return new ExecutionResponse(
                problem.getSlug(),
                kind.name(),
                report.outcome().name(),
                report.casesTotal(),
                report.casesPassed(),
                report.durationMs(),
                List.copyOf(results),
                errorDetail(report));
    }

    /**
     * The failure to show, phrased so it can be displayed verbatim.
     *
     * An internal error is replaced with our own words. The raw message can carry a container or
     * socket path, and someone debugging an algorithm should never be reading about our plumbing.
     */
    private static ExecutionResponse.ErrorDetail errorDetail(ExecutionReport report) {
        if (report.outcome() == ExecutionOutcome.ACCEPTED || report.outcome() == ExecutionOutcome.WRONG_ANSWER) {
            return null;
        }
        if (report.outcome() == ExecutionOutcome.INTERNAL_ERROR) {
            return new ExecutionResponse.ErrorDetail(
                    "RunnerUnavailable", null,
                    "The runner could not evaluate this. That is a problem on our side, not with your code.");
        }
        return new ExecutionResponse.ErrorDetail(
                report.errorType(),
                report.errorLine(),
                report.errorMessage() == null ? defaultMessage(report.outcome()) : report.errorMessage());
    }

    private static String defaultMessage(ExecutionOutcome outcome) {
        return switch (outcome) {
            case SYNTAX_ERROR -> "The solution could not be parsed, so it never ran.";
            case RUNTIME_ERROR -> "The solution raised an error while running.";
            case TIME_LIMIT_EXCEEDED -> "The solution did not finish in time.";
            case MEMORY_LIMIT_EXCEEDED -> "The solution ran out of memory.";
            default -> "The solution did not pass.";
        };
    }

    private static String clamp(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DISPLAY_CHARS ? value : value.substring(0, MAX_DISPLAY_CHARS) + "…";
    }

    private CodeExecutionEntity record(UserEntity user,
                                       AttemptEntity attempt,
                                       ProblemEntity problem,
                                       ExecuteCodeRequest request,
                                       ExecutionKind kind,
                                       ExecutionReport report) {
        CodeExecutionEntity entity = new CodeExecutionEntity();
        entity.setUser(user);
        entity.setAttemptId(attempt.getId());
        entity.setProblem(problem);
        entity.setKind(kind);
        entity.setLanguage(request.languageOrThrow());
        entity.setOutcome(report.outcome());
        entity.setCasesTotal(report.casesTotal());
        entity.setCasesPassed(report.casesPassed());
        entity.setDurationMs(report.durationMs() == null ? null : report.durationMs().intValue());
        entity.setSource(request.code());
        return entity;
    }

    private static ProblemEntity requireRunnable(ProblemEntity problem) {
        if (problem.getEntrypoint() == null || problem.getEntrypoint().isBlank()) {
            throw new CodeExecutionProvider.NotRunnableException(
                    "Problem " + problem.getSlug() + " is not available for coding yet");
        }
        return problem;
    }

    /**
     * The learner's saved source and the last thing the server concluded about it.
     *
     * Built from the entity, inside the caller's transaction, because the API runs with
     * open-in-view disabled and a lazy association read outside one would fail.
     */
    public record AttemptCodeState(
            String language,
            String code,
            String outcome,
            boolean accepted,
            int hintsUsed) {

        static AttemptCodeState of(AttemptEntity attempt) {
            return new AttemptCodeState(
                    attempt.getLanguage() == null ? null : attempt.getLanguage().name(),
                    attempt.getCode(),
                    attempt.getCodeOutcome() == null ? null : attempt.getCodeOutcome().name(),
                    attempt.isCodeAccepted(),
                    attempt.getHintsUsed());
        }
    }
}