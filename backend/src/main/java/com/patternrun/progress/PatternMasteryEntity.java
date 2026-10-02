package com.patternrun.progress;

import com.patternrun.pattern.PatternEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A learner's standing in one pattern.
 *
 * Each axis is nullable and NULL means "not measured yet", which is not the same as zero: a
 * learner who has never been reviewed has no retention score, while a learner who has failed a
 * review has a low one. The overall score is a weighted average over whichever axes exist
 * (README section 37), so an axis can start being measured later with no migration and no
 * backfill.
 */
@Entity
@Table(name = "user_pattern_mastery")
@Getter
@Setter
@NoArgsConstructor
public class PatternMasteryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.patternrun.account.UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pattern_id", nullable = false)
    private PatternEntity pattern;

    @Column(name = "recognition_score", precision = 5, scale = 2)
    private BigDecimal recognitionScore;

    @Column(name = "correctness_score", precision = 5, scale = 2)
    private BigDecimal correctnessScore;

    @Column(name = "speed_score", precision = 5, scale = 2)
    private BigDecimal speedScore;

    @Column(name = "explanation_score", precision = 5, scale = 2)
    private BigDecimal explanationScore;

    @Column(name = "retention_score", precision = 5, scale = 2)
    private BigDecimal retentionScore;

    @Column(name = "attempts_count", nullable = false)
    private int attemptsCount;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
