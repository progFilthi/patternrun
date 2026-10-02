package com.patternrun.progress;

import com.patternrun.attempt.AttemptEntity;
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
 * An immutable XP award. The learner's total is a sum over this table, so no running counter
 * exists to drift under retries, two tabs or an import.
 */
@Entity
@Table(name = "user_xp_awards")
@Getter
@Setter
@NoArgsConstructor
public class XpAwardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.patternrun.account.UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private AttemptEntity attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private ProblemEntity problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private XpReason reason;

    @Column(nullable = false)
    private int xp;

    /**
     * Idempotency key, unique per learner when present. A repeat award is rejected by the
     * database rather than by service logic, which is what makes a retried request safe.
     */
    @Column(name = "award_key", length = 160)
    private String awardKey;

    @CreationTimestamp
    @Column(name = "awarded_at", nullable = false, updatable = false)
    private Instant awardedAt;

    public XpAwardEntity(
            com.patternrun.account.UserEntity user,
            AttemptEntity attempt,
            ProblemEntity problem,
            XpReason reason,
            int xp,
            String awardKey) {
        this.user = user;
        this.attempt = attempt;
        this.problem = problem;
        this.reason = reason;
        this.xp = xp;
        this.awardKey = awardKey;
    }
}
