package com.patternrun.progress;

import com.patternrun.problem.ProblemEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A learner's fastest correct run of one problem. A speedrun only counts when the pattern was
 * identified correctly, so a fast wrong answer cannot become a record.
 */
@Entity
@Table(name = "user_speedrun_records")
@Getter
@Setter
@NoArgsConstructor
public class SpeedrunRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.patternrun.account.UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @Column(name = "best_duration_ms", nullable = false)
    private long bestDurationMs;

    @Column(name = "best_attempt_id")
    private UUID bestAttemptId;

    @Column(nullable = false)
    private int runs = 1;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isBetterThan(long candidateMs) {
        return candidateMs < bestDurationMs;
    }
}
