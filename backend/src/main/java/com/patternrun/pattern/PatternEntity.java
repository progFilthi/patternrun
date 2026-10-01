package com.patternrun.pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "patterns")
@Getter
@Setter
@NoArgsConstructor
public class PatternEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String summary;

    /** The signal that makes a user think of this pattern (README section 26). */
    @Column(nullable = false)
    private String signal;

    @Column(name = "mental_model", nullable = false)
    private String mentalModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recognition_rules", nullable = false)
    private List<String> recognitionRules = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> template = new ArrayList<>();

    /** The one property that must stay true for the pattern to be correct (README section 86). */
    @Column(nullable = false)
    private String invariant;

    @Column(name = "difficulty_order", nullable = false)
    private int difficultyOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}