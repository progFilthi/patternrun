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

/** A checkpoint in the test panel (README section 10). */
@Entity
@Table(name = "problem_test_cases")
@Getter
@Setter
@NoArgsConstructor
public class ProblemTestCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @Column(nullable = false)
    private Integer ordinal;

    @Column(nullable = false)
    private String label;

    @Column(name = "input_data", nullable = false)
    private String inputData;

    @Column(name = "expected_output", nullable = false)
    private String expectedOutput;

    /** Hidden cases are never returned by the public API (README section 47). */
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden;
}