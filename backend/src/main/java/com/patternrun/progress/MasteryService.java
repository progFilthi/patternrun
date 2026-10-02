package com.patternrun.progress;

import com.patternrun.account.UserEntity;
import com.patternrun.attempt.AttemptEntity;
import com.patternrun.attempt.AttemptRepository;
import com.patternrun.attempt.AttemptStatus;
import com.patternrun.pattern.PatternEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps {@code user_pattern_mastery} in step with the attempts behind it.
 *
 * Recomputed from scratch per pattern rather than nudged. A mastery row is a projection of the
 * attempts that justify it, so rebuilding it is both simpler and more trustworthy than
 * incrementing five scores: a bad increment cannot be undone, and a replayed import cannot
 * double-count. The cost is a handful of rows per attempt, which is nothing at this scale.
 */
@Service
public class MasteryService {

    private final PatternMasteryRepository masteries;
    private final AttemptRepository attempts;

    public MasteryService(PatternMasteryRepository masteries, AttemptRepository attempts) {
        this.masteries = masteries;
        this.attempts = attempts;
    }

    /**
     * Rebuilds the row for one pattern and returns it.
     *
     * @return empty when the learner has no completed attempts in the pattern, in which case no
     *     row is written: a zeroed bar for an untouched pattern is noise, not information
     */
    @Transactional
    public Optional<PatternMasteryEntity> recompute(UserEntity user, PatternEntity pattern) {
        List<AttemptEntity> history = MasteryScoring.completedOnly(
                attempts.findCompletedForPattern(user.getId(), pattern.getId()));
        if (history.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal recognition = MasteryScoring.recognition(history);
        BigDecimal correctness = MasteryScoring.correctness(history);
        BigDecimal speed = MasteryScoring.speed(history);

        PatternMasteryEntity row = masteries.findByUserIdAndPatternId(user.getId(), pattern.getId())
                .orElseGet(() -> {
                    PatternMasteryEntity fresh = new PatternMasteryEntity();
                    fresh.setUser(user);
                    fresh.setPattern(pattern);
                    return fresh;
                });

        // explanation and retention are deliberately left alone. explanation has no signal until
        // the breakdown step exists; retention has none until the review queue has history.
        // Writing a zero now would make a learner look bad at something never measured.
        row.setRecognitionScore(recognition);
        row.setCorrectnessScore(correctness);
        row.setSpeedScore(speed);
        row.setAttemptsCount(history.size());
        row.setOverallScore(MasteryScoring.overall(
                MasteryScoring.axesOf(recognition, correctness, speed,
                        row.getExplanationScore(), row.getRetentionScore())));

        return Optional.of(masteries.save(row));
    }

    /** The current overall for one pattern, for the completion summary to show. */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> overall(UUID userId, UUID patternId) {
        return masteries.findByUserIdAndPatternId(userId, patternId)
                .map(PatternMasteryEntity::getOverallScore);
    }

    @Transactional(readOnly = true)
    public List<PatternMasteryEntity> all(UUID userId) {
        return masteries.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<PatternMasteryEntity> find(UUID userId, UUID patternId) {
        return masteries.findByUserIdAndPatternId(userId, patternId);
    }
}