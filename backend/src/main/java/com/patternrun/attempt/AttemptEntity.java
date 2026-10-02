package com.patternrun.attempt;

import com.patternrun.problem.ProblemEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One training session, append-only.
 *
 * Repeating a problem adds a row rather than overwriting, which is what the mistake journal,
 * spaced repetition and speedrun personal bests all read from. The correctness flags are
 * authored by the server from the learner's observations, never by the client.
 */
/** One breakdown answer, as recorded. */
record BreakdownAnswer(String key, int chosenIndex) {
}

@Entity
@Table(name = "user_problem_attempts")
@Getter
@Setter
@NoArgsConstructor
public class AttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.patternrun.account.UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    /** Denormalised from {@code problems.primary_pattern_id} so mastery avoids a join. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pattern_id", nullable = false)
    private com.patternrun.pattern.PatternEntity pattern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptMode mode = AttemptMode.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptSource source = AttemptSource.LIVE;

    @Enumerated(EnumType.STRING)
    private com.patternrun.problem.ProgrammingLanguage language;

    @Column(columnDefinition = "text")
    private String code;

    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    @Column(name = "pattern_guess")
    private String patternGuess;

    @Column(name = "pattern_correct", nullable = false)
    private boolean patternCorrect;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<PredictionRecord> predictions = new ArrayList<>();

    @Column(name = "complexity_time_guess")
    private String complexityTimeGuess;

    @Column(name = "complexity_space_guess")
    private String complexitySpaceGuess;

    @Column(name = "complexity_correct", nullable = false)
    private boolean complexityCorrect;

    /** What the learner chose in the breakdown step, as [{key, chosenIndex}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "breakdown_answers", nullable = false)
    private List<BreakdownAnswer> breakdownAnswers = new ArrayList<>();

    /**
     * Whether the learner read the problem back correctly.
     *
     * Derived from {@code breakdownAnswers} when those are recorded, never from a flag sent by
     * the browser. A client-supplied correctness flag would be the learner marking their own
     * work, and it would be worth twenty XP.
     */
    @Column(name = "breakdown_correct", nullable = false)
    private boolean breakdownCorrect;

    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * What this single attempt paid out. Denormalised so one attempt's result reads without
     * joining the ledger; the learner's total is a sum over {@code user_xp_awards}, never this
     * column, so nothing has to be kept in step.
     */
    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isCompleted() {
        return status == AttemptStatus.COMPLETED;
    }

    /** A prediction counts as correct only when every question was answered and all were right. */
    public boolean predictionsAllCorrect() {
        return !predictions.isEmpty()
                && predictions.stream().allMatch(PredictionRecord::correct);
    }
}