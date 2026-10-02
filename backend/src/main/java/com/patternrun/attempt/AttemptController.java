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
import com.patternrun.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The training session endpoints (README section 48).
 *
 * Paths are exactly the ones the spec named, so the contract existed before the code. Every
 * handler requires a learner: browsing content anonymously is the normal case, but a session
 * that pays XP has to belong to someone.
 *
 * Thin on purpose. The services return response records, because the API runs with open-in-view
 * disabled and anything assembled in the controller would be reading lazy associations after
 * their transaction had closed.
 */
@RestController
@RequestMapping("/api/v1/attempts")
public class AttemptController {

    private final AttemptService attempts;

    public AttemptController(AttemptService attempts) {
        this.attempts = attempts;
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

    @PostMapping("/{id}/complete")
    public CompletionResultResponse complete(
            @CurrentUser UserEntity user,
            @PathVariable UUID id,
            @Valid @RequestBody CompleteAttemptRequest request) {
        return attempts.complete(user, id, request);
    }
}