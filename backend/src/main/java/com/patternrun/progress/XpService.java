package com.patternrun.progress;

import com.patternrun.attempt.AttemptEntity;
import com.patternrun.problem.ProblemEntity;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only class permitted to write XP.
 *
 * That restriction is the whole answer to "XP must be calculated by the backend": not a
 * convention, an access boundary. Every award goes through {@link #award}, which inserts into an
 * append-only ledger under a unique idempotency key.
 *
 * The consequence is that repeating a problem pays out once. {@code completed:{problemId}} is
 * spent by the first completion and the second insert violates
 * {@code uq_xp_award_once}, which is caught and returned as a no-op. A learner who solves one
 * problem fifty times earns 50 XP, not 2500, and not because a service remembered to check but
 * because the repeat is unrepresentable.
 *
 * The same mechanism makes a retried request safe. A client that times out on
 * {@code POST /complete} and retries cannot double-credit, because the retry collides with the
 * key its first attempt already used.
 */
@Service
public class XpService {

    private final XpAwardRepository awards;

    public XpService(XpAwardRepository awards) {
        this.awards = awards;
    }

    /** Idempotency keys. Centralised so the shape of "once ever" is readable in one place. */
    public static final class Keys {

        private Keys() {
        }

        public static String patternIdentified(ProblemEntity problem) {
            return "pattern:" + problem.getId();
        }

        public static String problemCompleted(ProblemEntity problem) {
            return "completed:" + problem.getId();
        }

        public static String noHints(ProblemEntity problem) {
            return "nohints:" + problem.getId();
        }

        public static String correctComplexity(ProblemEntity problem) {
            return "complexity:" + problem.getId();
        }

        public static String problemBreakdown(ProblemEntity problem) {
            return "breakdown:" + problem.getId();
        }

        public static String bossDefeated(ProblemEntity problem) {
            return "boss:" + problem.getId();
        }

        /**
         * Predictions pay once per question per problem, not per attempt.
         *
         * Predicting correctly teaches something the first time and nothing the twentieth. The
         * accuracy rate across attempts is still measured, and it feeds the correctness half of
         * mastery, so nothing is lost by paying for it only once: keying this per attempt let a
         * learner farm a few XP per repeat indefinitely.
         */
        public static String prediction(String problemId, int stepOrder) {
            return "prediction:" + problemId + ":" + stepOrder;
        }

        public static String comboBonus(String attemptId) {
            return "combo:" + attemptId;
        }

        public static String dailyQuest(String userId, String day) {
            return "quest:" + userId + ":" + day;
        }

        public static String comeback(String mistakeId) {
            return "comeback:" + mistakeId;
        }

        public static String review(String mistakeId, int reviewNumber) {
            return "review:" + mistakeId + ":" + reviewNumber;
        }
    }

    /**
     * Awards XP once for a key. Returns the award, or null when the key was already spent.
     *
     * Runs in its own transaction so that losing the race for a key rolls back only this insert
     * and not the surrounding completion.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public XpAwardEntity award(
            com.patternrun.account.UserEntity user,
            AttemptEntity attempt,
            ProblemEntity problem,
            XpReason reason,
            String awardKey) {
        return award(user, attempt, problem, reason, reason.xp(), awardKey);
    }

    /**
     * Awards an explicit amount under a key.
     *
     * For the one award whose value is not fixed by its reason: a combo bonus scales with the
     * run, so {@code COMBO_BONUS} carries no amount of its own.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public XpAwardEntity award(
            com.patternrun.account.UserEntity user,
            AttemptEntity attempt,
            ProblemEntity problem,
            XpReason reason,
            int xp,
            String awardKey) {
        if (xp <= 0) {
            return null;
        }
        if (awardKey != null && awards.existsByUserIdAndAwardKey(user.getId(), awardKey)) {
            return null;
        }
        try {
            return awards.saveAndFlush(new XpAwardEntity(user, attempt, problem, reason, xp, awardKey));
        } catch (DataIntegrityViolationException alreadyAwarded) {
            // Another request spent the key between the check and the insert. That is the
            // outcome we wanted, so it is not an error.
            return null;
        }
    }

    /**
     * Awards XP with no key, for the one case a key cannot express.
     *
     * A personal best is meant to pay out every time it improves, so it is guarded by comparing
     * against the stored record under a lock rather than by a key. Callers must only reach here
     * after that comparison, which {@code AttemptService} owns.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public XpAwardEntity awardUnkeyed(
            com.patternrun.account.UserEntity user,
            AttemptEntity attempt,
            ProblemEntity problem,
            XpReason reason,
            int xp) {
        return awards.saveAndFlush(new XpAwardEntity(user, attempt, problem, reason, xp, null));
    }

    @Transactional(readOnly = true)
    public long totalXp(java.util.UUID userId) {
        return awards.totalXp(userId);
    }

    @Transactional(readOnly = true)
    public List<XpAwardEntity> awardsForAttempt(java.util.UUID attemptId) {
        return awards.findByAttemptIdOrderByAwardedAtAsc(attemptId);
    }
}