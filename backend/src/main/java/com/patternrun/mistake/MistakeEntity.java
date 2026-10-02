package com.patternrun.mistake;

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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * An open mistake and its review schedule.
 *
 * Leitner intervals: a correct review doubles the gap up to a ceiling, a wrong one resets it to
 * tomorrow. The schedule is advanced when a review happens rather than by a scheduled job, so
 * no scheduler has to be introduced into the application.
 */
@Entity
@Table(name = "user_mistakes")
@Getter
@Setter
@NoArgsConstructor
public class MistakeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.patternrun.account.UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @Column(name = "attempt_id")
    private UUID attemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MistakeCategory category;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, columnDefinition = "text")
    private String lesson;

    /** Set when a later attempt gets the problem fully right. */
    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static final int MAX_INTERVAL_DAYS = 60;

    public void markReviewed(boolean correct, Instant now) {
        reviewCount++;
        lastReviewedAt = now;
        intervalDays = correct ? Math.min(intervalDays * 2, MAX_INTERVAL_DAYS) : 1;
        dueAt = now.plusSeconds(intervalDays * 86_400L);
    }
}
