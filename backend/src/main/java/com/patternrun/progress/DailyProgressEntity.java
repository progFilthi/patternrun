package com.patternrun.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One learner-local day. `day` is computed server-side in the learner's own zone, so a client
 * cannot pick its own day boundary and quietly rewrite its streak.
 *
 * The streak itself is not stored here. It is derived from consecutive goal_met days, which
 * cannot drift out of step with the rows that justify it.
 */
@Entity
@Table(name = "user_daily_progress")
@Getter
@Setter
@NoArgsConstructor
public class DailyProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.patternrun.account.UserEntity user;

    @Column(nullable = false)
    private LocalDate day;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    @Column(name = "problems_completed", nullable = false)
    private int problemsCompleted;

    @Column(name = "patterns_identified", nullable = false)
    private int patternsIdentified;

    @Column(name = "mistakes_reviewed", nullable = false)
    private int mistakesReviewed;

    @Column(name = "speedruns_completed", nullable = false)
    private int speedrunsCompleted;

    @Column(name = "active_minutes", nullable = false)
    private int activeMinutes;

    /** The daily goal is one problem or ten minutes (README section 34). */
    @Column(name = "goal_met", nullable = false)
    private boolean goalMet;

    @Column(name = "quest_completed", nullable = false)
    private boolean questCompleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Folds one day of activity in and re-evaluates the goal and the quest. */
    public void record(DailyProgressScoring.DailyCounts counts, int activeMinutes, int xpEarned) {
        patternsIdentified = counts.patternsIdentified();
        problemsCompleted = counts.problemsCompleted();
        mistakesReviewed = counts.mistakesReviewed();
        speedrunsCompleted = counts.speedrunsCompleted();
        this.activeMinutes = activeMinutes;
        this.xpEarned = xpEarned;
        goalMet = DailyProgressScoring.meetsGoal(problemsCompleted, activeMinutes);
        questCompleted = DailyProgressScoring.questCompleted(counts);
    }
}
