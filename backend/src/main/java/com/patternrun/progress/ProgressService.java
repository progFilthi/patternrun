package com.patternrun.progress;

import com.patternrun.account.UserEntity;
import com.patternrun.attempt.AttemptEntity;
import com.patternrun.attempt.AttemptRepository;
import com.patternrun.attempt.AttemptStatus;
import com.patternrun.mistake.MistakeEntity;
import com.patternrun.mistake.MistakeRepository;
import com.patternrun.pattern.PatternEntity;
import com.patternrun.pattern.PatternRepository;
import com.patternrun.problem.ProblemEntity;
import com.patternrun.progress.dto.PatternMasteryResponse;
import com.patternrun.progress.dto.ProblemProgressResponse;
import com.patternrun.progress.dto.ProgressSummaryResponse;
import com.patternrun.progress.dto.ReviewItemResponse;
import com.patternrun.progress.dto.StreakResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading a learner's progress.
 *
 * All derived numbers come from stored rows rather than from counters, so a read can never
 * disagree with the ledger or the attempt history behind it. That is slower than a cached
 * integer and it is correct without a cache invalidation strategy.
 */
@Service
public class ProgressService {

    private final XpAwardRepository xpAwards;
    private final PatternMasteryRepository masteries;
    private final AttemptRepository attempts;
    private final DailyProgressRepository days;
    private final MistakeRepository mistakes;
    private final SpeedrunRecordRepository speedruns;
    private final PatternRepository patterns;
    private final DailyProgressService daily;

    public ProgressService(
            XpAwardRepository xpAwards,
            PatternMasteryRepository masteries,
            AttemptRepository attempts,
            DailyProgressRepository days,
            MistakeRepository mistakes,
            SpeedrunRecordRepository speedruns,
            PatternRepository patterns,
            DailyProgressService daily) {
        this.xpAwards = xpAwards;
        this.masteries = masteries;
        this.attempts = attempts;
        this.days = days;
        this.mistakes = mistakes;
        this.speedruns = speedruns;
        this.patterns = patterns;
        this.daily = daily;
    }

    /** Everything the dashboard and header need, in one response. */
    @Transactional(readOnly = true)
    public ProgressSummaryResponse summary(UserEntity user) {
        long total = xpAwards.totalXp(user.getId());
        List<PatternMasteryResponse> byPattern = masteryList(user);
        DailyProgressEntity today = daily.today(user);

        return new ProgressSummaryResponse(
                total,
                LevelScoring.levelNumber(total),
                LevelScoring.levelName(total),
                LevelScoring.levelProgress(total),
                daily.currentStreak(user),
                daily.longestStreak(user),
                daily.metToday(user),
                today == null ? null : new ProgressSummaryResponse.DailyProgressResponse(
                        today.getDay(),
                        today.getXpEarned(),
                        today.getProblemsCompleted(),
                        today.getPatternsIdentified(),
                        today.getMistakesReviewed(),
                        today.getSpeedrunsCompleted(),
                        today.getActiveMinutes(),
                        today.isGoalMet(),
                        today.isQuestCompleted(),
                        DailyProgressScoring.questItemsCompleted(new DailyProgressScoring.DailyCounts(
                                today.getPatternsIdentified(),
                                today.getProblemsCompleted(),
                                today.getMistakesReviewed(),
                                today.getSpeedrunsCompleted()))),
                byPattern,
                attempts.findAllCompleted(user.getId()).stream()
                        .map(attempt -> attempt.getProblem().getId())
                        .distinct()
                        .count(),
                mistakes.countByUserIdAndResolvedFalse(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<PatternMasteryResponse> masteryList(UserEntity user) {
        Map<UUID, PatternEntity> known = patterns.findAll().stream()
                .collect(Collectors.toMap(PatternEntity::getId, Function.identity()));

        return masteries.findByUserId(user.getId()).stream()
                .map(row -> {
                    PatternEntity pattern = known.get(row.getPattern().getId());
                    return new PatternMasteryResponse(
                            pattern == null ? null : pattern.getSlug(),
                            pattern == null ? null : pattern.getName(),
                            pattern == null ? 0 : pattern.getDifficultyOrder(),
                            row.getRecognitionScore(),
                            row.getCorrectnessScore(),
                            row.getExplanationScore(),
                            row.getSpeedScore(),
                            row.getRetentionScore(),
                            row.getOverallScore(),
                            row.getAttemptsCount());
                })
                .sorted(Comparator.comparing(PatternMasteryResponse::difficultyOrder))
                .toList();
    }

    @Transactional(readOnly = true)
    public StreakResponse streak(UserEntity user) {
        List<LocalDate> met = days.findByUserIdAndGoalMetTrueOrderByDayDesc(user.getId()).stream()
                .map(DailyProgressEntity::getDay)
                .toList();
        return new StreakResponse(
                DailyProgressScoring.currentStreak(met, DailyProgressService.todayFor(user)),
                DailyProgressScoring.longestStreak(met),
                DailyProgressScoring.isActiveToday(met, DailyProgressService.todayFor(user)),
                DailyProgressService.todayFor(user),
                met);
    }

    /** Per-problem state for the library, keyed by slug. Problems never attempted are omitted. */
    @Transactional(readOnly = true)
    public List<ProblemProgressResponse> problemStates(UserEntity user) {
        List<AttemptEntity> history = attempts.findByUserIdAndStatusOrderByCompletedAtDesc(
                user.getId(), AttemptStatus.COMPLETED);

        Map<UUID, Long> counts = history.stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getProblem().getId(), Collectors.counting()));

        Map<UUID, Long> attemptsPerProblem = attempts.findAllCompleted(user.getId()).stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getProblem().getId(), Collectors.counting()));

        Map<UUID, String> lastGrade = history.stream()
                .collect(Collectors.toMap(
                        attempt -> attempt.getProblem().getId(),
                        attempt -> MasteryScoring.grade(attempt),
                        (first, ignored) -> first));

        Map<UUID, Long> best = speedruns.findByUserIdOrderByBestDurationMsAsc(user.getId()).stream()
                .collect(Collectors.toMap(
                        record -> record.getProblem().getId(),
                        record -> record.getBestDurationMs(),
                        (first, ignored) -> first));

        return history.stream()
                .collect(Collectors.toMap(
                        attempt -> attempt.getProblem().getId(),
                        Function.identity(),
                        (first, ignored) -> first))
                .entrySet().stream()
                .map(entry -> {
                    UUID problemId = entry.getKey();
                    return new ProblemProgressResponse(
                            entry.getValue().getProblem().getSlug(),
                            attemptsPerProblem.getOrDefault(problemId, 0L).intValue(),
                            counts.getOrDefault(problemId, 0L) > 0,
                            lastGrade.get(problemId),
                            best.get(problemId),
                            counts.getOrDefault(problemId, 0L).intValue());
                })
                .sorted(Comparator.comparing(ProblemProgressResponse::problemSlug))
                .toList();
    }

    /**
     * Mistakes due for review, soonest first.
     *
     * Ordered by due date rather than by how badly they went wrong. A review queue that leads
     * with the learner's worst moment is a queue they avoid opening.
     */
    @Transactional(readOnly = true)
    public List<ReviewItemResponse> reviewQueue(UserEntity user) {
        Instant now = Instant.now();
        Map<UUID, ProblemEntity> known = attempts.findAllCompleted(user.getId()).stream()
                .map(AttemptEntity::getProblem)
                .collect(Collectors.toMap(ProblemEntity::getId, Function.identity(),
                        (first, ignored) -> first));

        return mistakes.findByUserIdAndResolvedFalseAndDueAtLessThanEqualOrderByDueAtAsc(user.getId(), now)
                .stream()
                .map(mistake -> toReviewItem(mistake, known))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<ReviewItemResponse> toReviewItem(
            MistakeEntity mistake, Map<UUID, ProblemEntity> known) {
        ProblemEntity problem = known.get(mistake.getProblem().getId());
        if (problem == null) {
            return Optional.empty();
        }
        return Optional.of(new ReviewItemResponse(
                mistake.getId().toString(),
                problem.getSlug(),
                problem.getTitle(),
                mistake.getCategory().name(),
                mistake.getDescription(),
                mistake.getLesson(),
                mistake.getReviewCount(),
                mistake.getIntervalDays(),
                mistake.getDueAt(),
                0));
    }
}