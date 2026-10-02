package com.patternrun.attempt;

import com.patternrun.account.UserEntity;
import com.patternrun.attempt.dto.AttemptResponse;
import com.patternrun.attempt.dto.CompleteAttemptRequest;
import com.patternrun.attempt.dto.CompletionResultResponse;
import com.patternrun.attempt.dto.BreakdownEvaluationResponse;
import com.patternrun.attempt.dto.RecordCodeRequest;
import com.patternrun.attempt.dto.SubmitBreakdownRequest;
import com.patternrun.attempt.dto.RecordHintRequest;
import com.patternrun.attempt.dto.RecordPatternRequest;
import com.patternrun.attempt.dto.RecordPredictionRequest;
import com.patternrun.attempt.dto.StartAttemptRequest;
import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.mistake.MistakeEntity;
import com.patternrun.mistake.MistakeRepository;
import com.patternrun.problem.AnimationStepEntity;
import com.patternrun.problem.AnimationStepRepository;
import com.patternrun.problem.BreakdownPrompt;
import com.patternrun.problem.ProgrammingLanguage;
import com.patternrun.problem.ProblemEntity;
import com.patternrun.problem.ProblemRepository;
import com.patternrun.progress.DailyProgressService;
import com.patternrun.progress.LevelScoring;
import com.patternrun.progress.MasteryScoring;
import com.patternrun.progress.MasteryService;
import com.patternrun.progress.SpeedrunRecordEntity;
import com.patternrun.progress.SpeedrunRecordRepository;
import com.patternrun.progress.XpAwardEntity;
import com.patternrun.progress.XpReason;
import com.patternrun.progress.XpService;
import com.patternrun.security.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The training session lifecycle, and the one place XP is decided.
 *
 * Two ideas hold the design together.
 *
 * <p><b>The client reports, the server judges.</b> A request carries what the learner chose, not
 * whether they were right. {@link CorrectnessChecking} decides every flag, because the answer to
 * each prediction is already inside the animation payload the browser received and a client
 * asserting its own correctness would be asserting something checkable.
 *
 * <p><b>Repeats never pay twice.</b> Every award carries an idempotency key, and the unique index
 * on it makes a second award for the same thing impossible rather than merely skipped. Solving a
 * problem fifty times earns fifty XP once, and a retried completion cannot double-credit. The
 * attempt still lands in the history, because the history is how mastery is computed and how a
 * learner can see they improved.
 */
@Service
public class AttemptService {

    private static final Logger log = LoggerFactory.getLogger(AttemptService.class);

    private final AttemptRepository attempts;
    private final ProblemRepository problems;
    private final AnimationStepRepository animationSteps;
    private final XpService xp;
    private final MasteryService mastery;
    private final DailyProgressService daily;
    private final SpeedrunRecordRepository speedruns;
    private final MistakeRepository mistakes;

    public AttemptService(
            AttemptRepository attempts,
            ProblemRepository problems,
            AnimationStepRepository animationSteps,
            XpService xp,
            MasteryService mastery,
            DailyProgressService daily,
            SpeedrunRecordRepository speedruns,
            MistakeRepository mistakes) {
        this.attempts = attempts;
        this.problems = problems;
        this.animationSteps = animationSteps;
        this.xp = xp;
        this.mastery = mastery;
        this.daily = daily;
        this.speedruns = speedruns;
        this.mistakes = mistakes;
    }

    /**
     * Opens a session, or hands back the one already open.
     *
     * Idempotent on purpose: a refresh in the middle of a session should resume it, not fail.
     * A second tab gets the same attempt rather than a conflict, and the unique index on live
     * attempts means the database agrees.
     */
    @Transactional
    public AttemptResponse start(UserEntity user, StartAttemptRequest request) {
        ProblemEntity problem = requireProblem(request.problemSlug());
        AttemptMode mode = parseMode(request.mode());

        Optional<AttemptEntity> live = attempts.findByUserIdAndProblemIdAndStatus(
                user.getId(), problem.getId(), AttemptStatus.IN_PROGRESS);
        if (live.isPresent()) {
            return AttemptResponse.of(live.get());
        }

        AttemptEntity attempt = new AttemptEntity();
        attempt.setUser(user);
        attempt.setProblem(problem);
        attempt.setPattern(problem.getPrimaryPattern());
        attempt.setMode(mode);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setSource(AttemptSource.LIVE);
        return AttemptResponse.of(attempts.save(attempt));
    }

    /**
     * Records the pattern the learner committed to, and decides whether it was right.
     *
     * Written before the walkthrough reveals the answer, which is the whole point of the step:
     * the guess has to be made while it is still a guess. Stored as a slug and judged here
     * against the problem's primary pattern, so the client cannot self-report a correct read.
     */
    @Transactional
    public AttemptResponse recordPattern(
            UserEntity user, UUID attemptId, RecordPatternRequest request) {
        AttemptEntity attempt = requireLive(user, attemptId);
        ProblemEntity problem = attempt.getProblem();

        attempt.setPatternGuess(request.patternSlug());
        attempt.setPatternCorrect(CorrectnessChecking.patternCorrect(
                request.patternSlug(), problem.getPrimaryPattern().getSlug()));
        return AttemptResponse.of(attempts.save(attempt));
    }

    /** Records a hint rung. Recorded as revealed, so hint dependency is evidence, not a claim. */
    @Transactional
    public AttemptResponse recordHint(UserEntity user, UUID attemptId, RecordHintRequest request) {
        AttemptEntity attempt = requireLive(user, attemptId);
        attempt.setHintsUsed(Math.max(attempt.getHintsUsed(), request.level()));
        return AttemptResponse.of(attempts.save(attempt));
    }

    /**
     * Records a prediction and decides whether it was right.
     *
     * The learner's option index is compared against the answer the animation step already
     * holds. The correct flag stored here is therefore the server's opinion, and the completion
     * payload cannot influence it.
     */
    @Transactional
    public AttemptResponse recordPrediction(
            UserEntity user, UUID attemptId, RecordPredictionRequest request) {
        AttemptEntity attempt = requireLive(user, attemptId);

        Integer answerIndex = answerIndexForQuestion(attempt.getProblem(), request.stepOrder());
        if (answerIndex == null) {
            throw new ResourceNotFoundException(
                    "No prediction question at step " + request.stepOrder());
        }

        List<PredictionRecord> predictions = new ArrayList<>(attempt.getPredictions());
        predictions.removeIf(record -> record.stepOrder() == request.stepOrder());
        predictions.add(new PredictionRecord(
                request.stepOrder(),
                request.chosenIndex(),
                CorrectnessChecking.predictionCorrect(request.chosenIndex(), answerIndex)));
        predictions.sort(java.util.Comparator.comparingInt(PredictionRecord::stepOrder));
        attempt.setPredictions(predictions);

        return AttemptResponse.of(attempts.save(attempt));
    }

    /**
     * Records what the learner chose for each breakdown prompt, and decides whether they read
     * the problem correctly.
     *
     * All-or-nothing across every prompt. Identifying what you are given but not what you must
     * return means the statement has not been read, and that is exactly the confusion this step
     * exists to catch.
     */
    @Transactional
    public BreakdownEvaluationResponse recordBreakdown(
            UserEntity user, UUID attemptId, SubmitBreakdownRequest request) {
        AttemptEntity attempt = requireLive(user, attemptId);
        List<BreakdownPrompt> prompts = attempt.getProblem().getBreakdown();
        if (prompts.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Problem " + attempt.getProblem().getSlug() + " has no breakdown yet");
        }

        Map<String, Integer> chosen = new HashMap<>();
        for (SubmitBreakdownRequest.Answer answer : request.answers()) {
            chosen.put(answer.key(), answer.chosenIndex());
        }

        List<BreakdownAnswer> recorded = new ArrayList<>();
        List<BreakdownEvaluationResponse.PromptResult> results = new ArrayList<>();
        int correctCount = 0;

        for (BreakdownPrompt prompt : prompts) {
            Integer picked = chosen.get(prompt.key());
            if (picked == null) {
                // An unanswered prompt makes the reading incomplete, which is a wrong answer
                // rather than a skipped one: the step is judged on whether it came out right.
                results.add(new BreakdownEvaluationResponse.PromptResult(
                        prompt.key(), prompt.prompt(), -1, false, prompt.explanation()));
                continue;
            }
            boolean right = picked.equals(prompt.answerIndex());
            if (right) {
                correctCount++;
            }
            recorded.add(new BreakdownAnswer(prompt.key(), picked));
            results.add(new BreakdownEvaluationResponse.PromptResult(
                    prompt.key(), prompt.prompt(), picked, right, prompt.explanation()));
        }

        attempt.setBreakdownAnswers(recorded);
        attempt.setBreakdownCorrect(correctCount == prompts.size());
        attempts.save(attempt);

        return new BreakdownEvaluationResponse(
                correctCount == prompts.size(), recorded.size(), prompts.size(), results);
    }

    /** Stores the learner's code. Never executed on this server (README section 66). */
    @Transactional
    public AttemptResponse recordCode(UserEntity user, UUID attemptId, RecordCodeRequest request) {
        AttemptEntity attempt = requireLive(user, attemptId);
        attempt.setLanguage(ProgrammingLanguage.valueOf(request.language()));
        attempt.setCode(request.code());
        return AttemptResponse.of(attempts.save(attempt));
    }

    /**
     * Finishes a session: derives every judgement, pays every award, then rebuilds what the
     * result implies.
     *
     * Order matters. The attempt is completed and saved before any XP is awarded, so the awards
     * can reference it, and the mastery and daily rows are rebuilt afterwards from the attempt
     * history rather than incremented.
     */
    @Transactional
    public CompletionResultResponse complete(
            UserEntity user, UUID attemptId, CompleteAttemptRequest request) {
        AttemptEntity attempt = attempts.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));

        if (attempt.isCompleted()) {
            // A retry of a completion that already landed. The ledger keys make the awards
            // no-ops anyway, so returning the recorded result is both safe and idempotent.
            return replayOutcome(user, attempt);
        }

        ProblemEntity problem = attempt.getProblem();

        // Computed before the attempt is marked complete. Counting afterwards would include this
        // attempt in its own run and make a first solve look like a combo of two.
        int combo = nextCombo(user);

        attempt.setComplexityTimeGuess(request.complexityTime());
        attempt.setComplexitySpaceGuess(request.complexitySpace());
        attempt.setComplexityCorrect(CorrectnessChecking.complexityCorrect(
                request.complexityTime(),
                request.complexitySpace(),
                problem.getTimeComplexity(),
                problem.getSpaceComplexity()));
        // Deliberately not set here. Whether the learner read the problem correctly was decided
        // when their breakdown answers were recorded, because that is the only point at which
        // the server holds the answer key. Reading it from this request would have made twenty
        // XP worth one boolean in a request body.
        attempt.setDurationMs(CorrectnessChecking.sanitiseDuration(request.durationMs()));
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setCompletedAt(Instant.now());
        if (request.language() != null) {
            attempt.setLanguage(ProgrammingLanguage.valueOf(request.language()));
        }
        attempts.save(attempt);

        List<XpAwardEntity> awards = new ArrayList<>();
        awardForCompletion(user, attempt, combo, awards);

        int earned = awards.stream().mapToInt(XpAwardEntity::getXp).sum();
        attempt.setXpAwarded(earned);
        attempts.save(attempt);

        boolean personalBest = updateSpeedrunBest(user, attempt);
        List<String> resolved = resolveMistakes(user, attempt, awards);

        mastery.recompute(user, attempt.getPattern());
        daily.recordCompletion(user, attempt.getCompletedAt());
        if (earned > 0) {
            daily.addXp(user, attempt.getCompletedAt(), earned);
        }
        awardDailyQuestIfMet(user, awards);

        log.debug("Learner {} completed {} earning {} xp", user.getId(), problem.getSlug(), earned);

        return toResult(attempt, awards, combo, personalBest, resolved);
    }

    /**
     * Shapes the completion response inside the transaction.
     *
     * Reading {@code attempt.getProblem().getSlug()} after the transaction closes would be a
     * lazy initialisation failure, since the API runs with open-in-view disabled. Building the
     * response here also matches the content layer, where services return records rather than
     * entities.
     */
    private CompletionResultResponse toResult(
            AttemptEntity attempt,
            List<XpAwardEntity> awards,
            int combo,
            boolean personalBest,
            List<String> resolved) {
        int predictionsCorrect = (int) attempt.getPredictions().stream()
                .filter(prediction -> prediction.correct())
                .count();

        return new CompletionResultResponse(
                attempt.getId(),
                attempt.getProblem().getSlug(),
                MasteryScoring.gradeFor(MasteryScoring.scoreOutOf100(attempt)),
                MasteryScoring.isProvisionalGrade(attempt),
                attempt.isPatternCorrect(),
                attempt.isComplexityCorrect(),
                attempt.isBreakdownCorrect(),
                attempt.getPredictions().size(),
                predictionsCorrect,
                attempt.getHintsUsed(),
                attempt.getDurationMs(),
                attempt.getXpAwarded(),
                combo,
                awards.stream()
                        .map(award -> new CompletionResultResponse.XpAwardResponse(
                                award.getReason().name(),
                                describe(award.getReason()),
                                award.getXp(),
                                false))
                        .toList(),
                attempt.getPattern().getSlug(),
                mastery.overall(attempt.getUser().getId(), attempt.getPattern().getId())
                        .map(BigDecimal::doubleValue)
                        .orElse(null),
                personalBest,
                resolved);
    }

    /**
     * Plain words for each award.
     *
     * A list of enum names on screen tells a learner nothing, and the mapping lives beside the
     * award logic so the two cannot drift apart.
     */
    public static String describe(XpReason reason) {
        return switch (reason) {
            case PATTERN_IDENTIFIED -> "Pattern identified";
            case PREDICTION_CORRECT -> "Prediction correct";
            case PROBLEM_COMPLETED -> "Problem solved";
            case COMBO_BONUS -> "Combo bonus";
            case NO_HINTS -> "No hints needed";
            case CORRECT_COMPLEXITY -> "Complexity correct";
            case PROBLEM_BREAKDOWN -> "Read the problem correctly";
            case SPEEDRUN_PB -> "New personal best";
            case BOSS_DEFEATED -> "Boss defeated";
            case REVIEW_COMPLETED -> "Mistake reviewed";
            case DAILY_QUEST -> "Daily quest complete";
            case COMEBACK -> "Fixed a past mistake";
        };
    }

    /**
     * The awards for one completion.
     *
     * Every key here is once-ever per problem, which is the whole of the anti-abuse rule. The
     * multiplier is applied to the completion award alone: a bonus for a run must not also pay
     * out for skipping the parts that make the learning stick (README section 96).
     */
    private void awardForCompletion(
            UserEntity user, AttemptEntity attempt, int combo, List<XpAwardEntity> awards) {
        ProblemEntity problem = attempt.getProblem();

        if (attempt.isPatternCorrect()) {
            add(user, attempt, problem, XpReason.PATTERN_IDENTIFIED,
                    XpService.Keys.patternIdentified(problem), awards);
        }

        for (PredictionRecord prediction : attempt.getPredictions()) {
            if (prediction.correct()) {
                add(user, attempt, problem, XpReason.PREDICTION_CORRECT,
                        XpService.Keys.prediction(problem.getId().toString(), prediction.stepOrder()),
                        awards);
            }
        }

        // Null means this problem was already solved before, which is the signal that decides
        // whether the run bonus applies.
        XpAwardEntity completion = xp.award(user, attempt, problem, XpReason.PROBLEM_COMPLETED,
                XpService.Keys.problemCompleted(problem));
        if (completion != null) {
            awards.add(completion);
        }

        // The run bonus is a separate reason with a per-attempt key, and only for genuinely new
        // material. A combo is a run through problems you have not solved yet, so re-solving one
        // neither earns the bonus nor extends the run. Paying it on repeats turned a single
        // problem into an unlimited XP source, which is exactly what the award keys exist to
        // prevent.
        boolean firstSolve = completion != null;
        if (combo > 1 && firstSolve) {
            int bonus = (int) Math.round(XpReason.PROBLEM_COMPLETED.xp() * (combo - 1));
            if (bonus > 0) {
                XpAwardEntity award = xp.award(user, attempt, problem, XpReason.COMBO_BONUS, bonus,
                        XpService.Keys.comboBonus(attempt.getId().toString()));
                if (award != null) {
                    awards.add(award);
                }
            }
        }

        if (attempt.getHintsUsed() == 0) {
            add(user, attempt, problem, XpReason.NO_HINTS,
                    XpService.Keys.noHints(problem), awards);
        }
        if (attempt.isComplexityCorrect()) {
            add(user, attempt, problem, XpReason.CORRECT_COMPLEXITY,
                    XpService.Keys.correctComplexity(problem), awards);
        }
        if (attempt.isBreakdownCorrect()) {
            add(user, attempt, problem, XpReason.PROBLEM_BREAKDOWN,
                    XpService.Keys.problemBreakdown(problem), awards);
        }
        if (attempt.getMode() == AttemptMode.BOSS) {
            add(user, attempt, problem, XpReason.BOSS_DEFEATED,
                    XpService.Keys.bossDefeated(problem), awards);
        }
        if (attempt.getMode() == AttemptMode.SPEEDRUN && attempt.isPatternCorrect()
                && attempt.getDurationMs() != null) {
            awards.addAll(speedrunAwards(user, attempt));
        }
    }

    /**
     * Consecutive correct completions today, counting this one.
     *
     * A wrong pattern breaks the run, and a break never takes back XP already earned or touches
     * the streak. Mistakes are data, not a punishment (README section 33).
     */
    private int nextCombo(UserEntity user) {
        ZoneId zone = DailyProgressService.zoneOf(user);
        Instant now = Instant.now();
        long alreadyCorrect = attempts.countCorrectCompletionsBetween(
                user.getId(),
                now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant(),
                now);
        // The multiplier doubles as the displayed combo count: 1x means a run of one.
        return (int) LevelScoring.comboMultiplier((int) (alreadyCorrect + 1));
    }

    /**
     * The personal best, and the one award a key cannot express.
     *
     * A personal best is meant to pay out every time it improves, so the guard is a comparison
     * under a row lock rather than a spent key. The record only counts a run that identified the
     * pattern correctly: a fast wrong answer is not a record.
     */
    private List<XpAwardEntity> speedrunAwards(UserEntity user, AttemptEntity attempt) {
        long duration = attempt.getDurationMs();
        SpeedrunRecordEntity record = speedruns
                .findForUpdate(user.getId(), attempt.getProblem().getId())
                .orElse(null);

        boolean improved = record == null || record.isBetterThan(duration);
        if (improved) {
            if (record == null) {
                record = new SpeedrunRecordEntity();
                record.setUser(user);
                record.setProblem(attempt.getProblem());
                record.setBestDurationMs(duration);
                record.setRuns(1);
            } else {
                record.setBestDurationMs(duration);
                record.setRuns(record.getRuns() + 1);
            }
            record.setBestAttemptId(attempt.getId());
            speedruns.save(record);
        }

        if (!improved) {
            return List.of();
        }
        XpAwardEntity award = xp.awardUnkeyed(user, attempt, attempt.getProblem(),
                XpReason.SPEEDRUN_PB, XpReason.SPEEDRUN_PB.xp());
        return award == null ? List.of() : List.of(award);
    }

    private boolean updateSpeedrunBest(UserEntity user, AttemptEntity attempt) {
        return attempt.getMode() == AttemptMode.SPEEDRUN && attempt.isPatternCorrect()
                && attempt.getDurationMs() != null;
    }

    /**
     * Closes any open mistake on this problem that the attempt has now fixed.
     *
     * Fixing a mistake is worth more than a fresh problem (README section 104), so this is the
     * one place a repeat earns something: the key is per mistake, so it can only ever pay once
     * for the same mistake.
     */
    private List<String> resolveMistakes(
            UserEntity user, AttemptEntity attempt, List<XpAwardEntity> awards) {
        boolean fullyCorrect = attempt.isPatternCorrect()
                && attempt.isComplexityCorrect()
                && attempt.isBreakdownCorrect()
                && attempt.predictionsAllCorrect();
        if (!fullyCorrect) {
            return List.of();
        }

        List<String> resolved = new ArrayList<>();
        for (MistakeEntity mistake : mistakes.findByUserIdAndProblemIdAndResolvedFalse(
                user.getId(), attempt.getProblem().getId())) {
            mistake.setResolved(true);
            mistakes.save(mistake);
            resolved.add(mistake.getCategory().name());

            XpAwardEntity award = xp.award(user, attempt, attempt.getProblem(),
                    XpReason.COMEBACK, XpService.Keys.comeback(mistake.getId().toString()));
            if (award != null) {
                awards.add(award);
                daily.recordReview(user, Instant.now());
            }
        }
        return resolved;
    }

    /** The daily quest pays once per day, enforced by its key rather than by a flag. */
    private void awardDailyQuestIfMet(UserEntity user, List<XpAwardEntity> awards) {
        var today = daily.today(user);
        if (today == null || !today.isQuestCompleted()) {
            return;
        }
        XpAwardEntity award = xp.award(user, null, null, XpReason.DAILY_QUEST,
                XpService.Keys.dailyQuest(user.getId().toString(), today.getDay().toString()));
        if (award != null) {
            awards.add(award);
        }
    }

    private void add(
            UserEntity user,
            AttemptEntity attempt,
            ProblemEntity problem,
            XpReason reason,
            String key,
            List<XpAwardEntity> awards) {
        XpAwardEntity award = xp.award(user, attempt, problem, reason, key);
        if (award != null) {
            awards.add(award);
        }
    }

    /** The recorded result of an already-completed attempt, for a safe retry. */
    private CompletionResultResponse replayOutcome(UserEntity user, AttemptEntity attempt) {
        return toResult(attempt, xp.awardsForAttempt(attempt.getId()), 1, false, List.of());
    }

    /** The answer index of the QUESTION step at this order, if there is one. */
    private Integer answerIndexForQuestion(ProblemEntity problem, int stepOrder) {
        for (AnimationStepEntity step : animationSteps
                .findByProblemIdOrderByStepOrderAsc(problem.getId())) {
            if (step.getStepOrder() == stepOrder) {
                Object value = step.getPayload().get("answerIndex");
                if (value instanceof Number number) {
                    return number.intValue();
                }
            }
        }
        return null;
    }

    private AttemptEntity requireLive(UserEntity user, UUID attemptId) {
        AttemptEntity attempt = attempts.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
        if (attempt.isCompleted()) {
            throw new ConflictException("That session is already finished.");
        }
        return attempt;
    }

    private ProblemEntity requireProblem(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("problemSlug is required");
        }
        return problems.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + slug));
    }

    private static AttemptMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return AttemptMode.STANDARD;
        }
        try {
            return AttemptMode.valueOf(mode);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("mode must be STANDARD, SPEEDRUN or BOSS");
        }
    }
}