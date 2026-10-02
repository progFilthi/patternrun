package com.patternrun.execution;

import com.patternrun.account.UserEntity;
import com.patternrun.problem.ProblemEntity;
import com.patternrun.problem.ProgrammingLanguage;
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
 * One execution of one learner's program, kept forever.
 *
 * The attempt holds a summary; this holds the ordered events behind it. That is the difference
 * between a table and a redundant one: "how many attempts did they need, what failed each time,
 * and did they solve it before or after using a hint" is a question about a sequence, and a
 * single summary column per attempt cannot answer it. Overwriting one row per attempt would throw
 * away exactly the history the product is built to learn from.
 *
 * Source is stored because a failure worth reviewing needs the code that caused it, and by the
 * time anyone looks the learner has moved on. It is their own code, not infrastructure.
 */
@Entity
@Table(name = "user_code_executions")
@Getter
@Setter
@NoArgsConstructor
public class CodeExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * A plain UUID rather than a mapped association, following {@code MistakeEntity.attemptId} and
     * {@code SpeedrunRecordEntity.bestAttemptId}.
     *
     * The execution package reads attempts through the service that owns them. Mapping the
     * association here would make the packages depend on each other's entities for the sake of a
     * foreign key the database already enforces.
 */
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgrammingLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionOutcome outcome;

    @Column(name = "cases_total", nullable = false)
    private int casesTotal;

    @Column(name = "cases_passed", nullable = false)
    private int casesPassed;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(nullable = false, columnDefinition = "text")
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}