package com.patternrun.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    private String content;
}