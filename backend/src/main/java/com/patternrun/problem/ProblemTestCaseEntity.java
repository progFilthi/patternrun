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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

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

    /**
     * Positional arguments for the problem's entrypoint, as a JSON array.
     *
     * This is what actually gets called. {@code inputData} is prose written for a human
     * ("nums = [2,7,11,15], target = 9") and is never parsed; keeping the runnable form separate
     * is what stops a negative number from being indistinguishable from a subtraction.
     *
     * Null means the case is display-only. A problem opts in to being runnable by having these
     * set; until then it is served as content and never executed.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "call")
    private JsonNode call;

    /** The expected return value, compared structurally rather than as a string. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_json")
    private JsonNode expectedJson;

    /** Whether this case can be handed to a runner. */
    public boolean isRunnable() {
        return call != null && expectedJson != null;
    }
}