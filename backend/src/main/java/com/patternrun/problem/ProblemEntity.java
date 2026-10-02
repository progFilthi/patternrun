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

    /**
     * Prompts that break the statement down into what is given, what is asked and which
     * constraint matters. Empty until the remaining problems are authored.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<BreakdownPrompt> breakdown = new ArrayList<>();

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

    /**
     * The function a runner calls, e.g. {@code two_sum}.
     *
     * Lives here rather than in the client on purpose. If the browser chose which function to
     * call it would also choose the argument shape, and "my code passed" would become a claim
     * about a call the learner defined. Null means the problem is not runnable yet, which is the
     * state for the nineteen problems that have display cases but no structured arguments.
     */
    @Column(name = "entrypoint")
    private String entrypoint;

    /**
     * How the runner turns stored arguments into a call.
     *
     * {@code PLAIN} for the eighteen problems that need nothing, {@code TREE} for the two whose
     * arguments are a level-order array rather than the object the entrypoint expects.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "argument_mode", nullable = false)
    private ArgumentMode argumentMode = ArgumentMode.PLAIN;

    /**
     * Whether the coding stage is available for this problem.
     *
     * Derived rather than stored, because it is a function of the entrypoint and the two could not
     * be allowed to disagree.
     */
    public boolean isRunnable() {
        return entrypoint != null && !entrypoint.isBlank();
    }

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