package com.patternrun.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.patternrun.pattern.PatternEntity;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
public class ProblemEntity {

    @Id
    private UUID id;

    /** LeetCode problem number. */
    @Column(name = "external_id", nullable = false, unique = true)
    private Integer externalId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_difficulty", nullable = false)
    private TrainingDifficulty trainingDifficulty;

    @Column(nullable = false)
    private String statement;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> constraints = new ArrayList<>();

    @Column(name = "why_this_pattern", nullable = false)
    private String whyThisPattern;

    @Column(name = "brute_force", nullable = false)
    private String bruteForce;

    @Column(nullable = false)
    private String invariant;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> pseudocode = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "common_mistakes", nullable = false)
    private List<String> commonMistakes = new ArrayList<>();

    @Column(name = "interview_explanation", nullable = false)
    private String interviewExplanation;

    @Column(name = "time_complexity", nullable = false)
    private String timeComplexity;

    @Column(name = "space_complexity", nullable = false)
    private String spaceComplexity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_pattern_id", nullable = false)
    private PatternEntity primaryPattern;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "problem_secondary_patterns",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "pattern_id"))
    private List<PatternEntity> secondaryPatterns = new ArrayList<>();

    @OneToMany(mappedBy = "problem", fetch = FetchType.LAZY)
    private List<ProblemExampleEntity> examples = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}