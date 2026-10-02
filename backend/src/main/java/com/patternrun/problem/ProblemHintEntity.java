package com.patternrun.problem;

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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One rung of the hint ladder (README section 7). */
@Entity
@Table(name = "problem_hints")
@Getter
@Setter
@NoArgsConstructor
public class ProblemHintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    /** 1 direction, 2 pattern, 3 structure, 4 pseudocode, 5 implementation. */
    @Column(nullable = false)
    private Integer level;

    /**
     * Which part of the loop this rung answers. Phase 4 addition; {@code ANY} for everything
     * authored before it, which keeps the ladder selection below total.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HintStage stage = HintStage.ANY;

    /** What must have happened first. A wrong-answer hint is noise during ordinary reasoning. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HintTrigger trigger = HintTrigger.ANY;

    @Column(nullable = false)
    private String content;

    /**
     * Whether this rung answers the moment the learner is in.
     *
     * A staged rung is specific: it wins for its own stage and trigger. An {@code ANY} rung is a
     * fallback, so it is only used when nothing more precise exists. Without that ordering rule,
     * the single generic ladder would shadow every purpose-built hint and the staging would be
     * decorative.
     */
    public boolean isSpecificTo(HintStage wantedStage, HintTrigger wantedTrigger) {
        boolean stageMatches = stage == HintStage.ANY || stage == wantedStage;
        boolean triggerMatches = trigger == HintTrigger.ANY || trigger == wantedTrigger;
        return stageMatches && triggerMatches;
    }
}