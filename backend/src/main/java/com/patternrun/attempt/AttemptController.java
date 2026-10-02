package com.patternrun.attempt;

import com.patternrun.account.UserEntity;
import com.patternrun.attempt.dto.AttemptResponse;
import com.patternrun.attempt.dto.BreakdownEvaluationResponse;
import com.patternrun.attempt.dto.CompleteAttemptRequest;
import com.patternrun.attempt.dto.CompletionResultResponse;
import com.patternrun.attempt.dto.RecordCodeRequest;
import com.patternrun.attempt.dto.RecordHintRequest;
import com.patternrun.attempt.dto.RecordPatternRequest;
import com.patternrun.attempt.dto.RecordPredictionRequest;
import com.patternrun.attempt.dto.StartAttemptRequest;
import com.patternrun.attempt.dto.SubmitBreakdownRequest;
import com.patternrun.execution.CodeExecutionService;
import com.patternrun.execution.CodeExecutionService.AttemptCodeState;
import com.patternrun.execution.SolutionRevealService;
import com.patternrun.execution.SolutionRevealService.RevealedSolution;
import com.patternrun.execution.dto.ExecutionResponse;
import com.patternrun.execution.dto.ExecuteCodeRequest;
import com.patternrun.problem.HintStage;
import com.patternrun.problem.ProgrammingLanguage;
import com.patternrun.problem.dto.HintSelection;
import com.patternrun.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The training session endpoints (README section 48).
 *
 * Paths are exactly the ones the spec named, so the contract existed before the code. Every
 * handler requires a learner: browsing content anonymously is the normal case, but a session
 * that pays XP has to belong to someone.
 *
 * Phase 4's coding endpoints hang off the same attempt rather than getting their own controller,
 * because a code submission is an event inside a training session, not a separate resource. Putting
 * them anywhere else would have meant inventing a second identity for the thing they describe.
 *
 * Thin on purpose. The services return response records, because the API runs with open-in-view
 * disabled and anything assembled in the controller would be reading lazy associations after
 * their transaction had closed.
 */
@RestController
@RequestMapping("/api/v1/attempts")
public class AttemptController {

    private final AttemptService attempts;
    private final CodeExecutionService codeExecution;
    private final SolutionRevealService solutions;

    public AttemptController(AttemptService attempts,
                             CodeExecutionService codeExecution,
                             SolutionRevealService solutions) {
        this.attempts = attempts;
        this.codeExecution = codeExecution;
        this.solutions = solutions;
    }

    @PostMapping
    public AttemptResponse start(
            @CurrentUser UserEntity user,
            @Valid @RequestBody StartAttemptRequest request) {
        return attempts.start(user, request);
    }

    @PostMapping("/{id}/pattern")
    public AttemptResponse pattern(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody RecordPatternRequest request) {
        return attempts.recordPattern(user, id, request);
    }

    @PostMapping("/{id}/hint")
    public AttemptResponse hint(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody RecordHintRequest request) {
        return attempts.recordHint(user, id, request);
    }

    @PostMapping("/{id}/predict")
    public AttemptResponse predict(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody RecordPredictionRequest request) {
        return attempts.recordPrediction(user, id, request);
    }

    @PostMapping("/{id}/breakdown")
    public BreakdownEvaluationResponse breakdown(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody SubmitBreakdownRequest request) {
        return attempts.recordBreakdown(user, id, request);
    }

    @PostMapping("/{id}/code")
    public AttemptResponse code(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody RecordCodeRequest request) {
        return attempts.recordCode(user, id, request);
    }

    /**
     * Runs the learner's code against the visible examples.
     *
     * Returns the result of the run and never any completion state. See {@link CodeExecutionService}
     * for why a Run cannot make a problem solved however well it goes.
     */
    @PostMapping("/{id}/code/run")
    public ExecutionResponse runCode(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody ExecuteCodeRequest request) {
        return codeExecution.run(user, id, request);
    }

    /** Evaluates against the hidden evaluation set. The only path that can accept a solution. */
    @PostMapping("/{id}/code/submit")
    public ExecutionResponse submitCode(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody ExecuteCodeRequest request) {
        return codeExecution.submit(user, id, request);
    }

    /** Autosave, so a refresh does not cost the learner their work. Decides nothing. */
    @PostMapping("/{id}/code/save")
    public AttemptCodeState saveCode(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody ExecuteCodeRequest request) {
        return codeExecution.save(user, id, request);
    }

    /** The saved source and the last verdict, so a reload can restore what was being written. */
    @GetMapping("/{id}/code")
    public AttemptCodeState codeState(@CurrentUser UserEntity user, @PathVariable UUID id) {
        return codeExecution.stateOf(user, id);
    }

    /**
     * The next hint for the moment the learner is actually in.
     *
     * The trigger is derived from the last execution the server observed. The client cannot ask for
     * a wrong-answer hint after a passing submission, which would be a way to walk straight to the
     * answer without ever having failed.
     */
    @GetMapping("/{id}/hint/next")
    public HintSelection nextHint(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "CODING") HintStage stage) {
        return codeExecution.nextHint(user, id, stage);
    }

    /** The last escape hatch. A POST, because obtaining the answer is itself a recorded event. */
    @PostMapping("/{id}/solution")
    public RevealedSolution revealSolution(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "PYTHON") ProgrammingLanguage language) {
        return solutions.reveal(user, id, language);
    }

    @PostMapping("/{id}/complete")
    public CompletionResultResponse complete(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody CompleteAttemptRequest request) {
        return attempts.complete(user, id, request);
    }
}