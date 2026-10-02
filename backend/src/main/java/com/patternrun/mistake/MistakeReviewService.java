package com.patternrun.mistake;

import com.patternrun.account.UserEntity;
import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.progress.DailyProgressService;
import com.patternrun.progress.XpReason;
import com.patternrun.security.ConflictException;
import com.patternrun.progress.XpService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closing the loop on a recorded mistake.
 *
 * Phase 3 built the spaced-repetition half of the mistake journal and never connected it: the
 * scheduling exists on {@link MistakeEntity#markReviewed}, the due-date query exists, and there was
 * no endpoint a learner could act on. A queue nobody can work through is worse than an empty one,
 * because it teaches them that the number is not worth looking at.
 *
 * <h3>What a review is</h3>
 *
 * A learner saying they have thought about the mistake again, and whether they now have it. That is
 * a claim about their own understanding, not a graded answer, so it is not something the backend can
 * verify --- and pretending otherwise would put a correctness judgement the client controls into the
 * progression model. What the backend owns is the consequence: the interval, the next due date, and
 * the XP.
 *
 * <h3>Why {@code REVIEW_COMPLETED} pays</h3>
 *
 * Because spacing is the behaviour being trained. An award that fires only when a learner is about
 * to forget and comes back is exactly the reward that makes them do it; one that pays for opening a
 * queue would not.
 */
@Service
public class MistakeReviewService {

    private final MistakeRepository mistakes;
    private final XpService xp;
    private final DailyProgressService daily;

    public MistakeReviewService(MistakeRepository mistakes, XpService xp, DailyProgressService daily) {
        this.mistakes = mistakes;
        this.xp = xp;
        this.daily = daily;
    }

/**
 * Reviews one mistake.
 *
 * @param correct whether the learner says they now have it. Drives the interval: right doubles
 *     it, wrong resets it to a day so the mistake comes back while it is still fresh.
 * @return the updated entry, as the client should show it.
 */
@Transactional
public ReviewOutcome review(UserEntity user, UUID mistakeId, boolean correct) {
    MistakeEntity mistake = mistakes.findByIdAndUserId(mistakeId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Mistake not found: " + mistakeId));

    if (mistake.isResolved()) {
        throw new ConflictException(
                "That mistake is already resolved. Nothing left to review.");
    }

    Instant now = Instant.now();

    // The check that makes this endpoint safe to expose.
    //
    // A review is only legitimate once the mistake has actually come due. Without that gate a
    // learner could call this repeatedly and collect REVIEW_COMPLETED every time, because the
    // award key includes the review number and the review number increments on every call. The
    // whole feature is spaced repetition, so allowing a review before it is due would let a learner
    // opt out of the spacing and keep the reward --- which defeats the reason it exists.
    //
    // It also makes a retried request harmless: the first call advanced the due date, so the retry
    // is refused rather than paid for twice.
    if (mistake.getDueAt().isAfter(now)) {
        throw new ConflictException(
                "That mistake is not due until " + mistake.getDueAt().atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate() + ".");
    }

    mistake.markReviewed(correct, now);
    mistakes.save(mistake);

    var award = xp.award(user, null, mistake.getProblem(), XpReason.REVIEW_COMPLETED,
            XpService.Keys.review(mistake.getId().toString(), mistake.getReviewCount()));
    if (award != null) {
        daily.recordReview(user, now);
        // The award is XP, so it has to reach today's total too. Recording the review without the
        // XP would leave "XP earned today" quietly disagreeing with the learner's actual total.
        daily.addXp(user, now, award.getXp());
    }

    return new ReviewOutcome(
            mistake.getId(),
            mistake.getProblem().getSlug(),
            mistake.getCategory().name(),
            correct,
            mistake.getReviewCount(),
            mistake.getIntervalDays(),
            mistake.getDueAt(),
            award == null ? 0 : award.getXp(),
            // A null award means the key was already spent. With the due-date gate above that is
            // only reachable if two requests raced, and the review itself still counted.
            award == null);
}

    /** Everything due now, soonest first. Read-only: reviewing is a separate, explicit act. */
    @Transactional(readOnly = true)
    public List<ReviewItem> due(UserEntity user, int limit) {
        Instant now = Instant.now();
        return mistakes.findByUserIdAndResolvedFalseAndDueAtLessThanEqualOrderByDueAtAsc(user.getId(), now)
                .stream()
                .limit(limit)
                .map(m -> new ReviewItem(
                        m.getId(),
                        m.getProblem().getSlug(),
                        m.getProblem().getTitle(),
                        m.getCategory().name(),
                        m.getDescription(),
                        m.getLesson(),
                        m.getReviewCount(),
                        m.getDueAt()))
                .toList();
    }

    /** How many are due, for a badge. */
    @Transactional(readOnly = true)
    public long dueCount(UserEntity user) {
        return mistakes
                .findByUserIdAndResolvedFalseAndDueAtLessThanEqualOrderByDueAtAsc(user.getId(), Instant.now())
                .size();
    }

    /** One item the learner should think about. */
    public record ReviewItem(
            UUID id,
            String problemSlug,
            String problemTitle,
            String category,
            String description,
            String lesson,
            int reviewCount,
            Instant dueAt) {
    }

    /** What one review did. */
    public record ReviewOutcome(
            UUID mistakeId,
            String problemSlug,
            String category,
            boolean correct,
            int reviewCount,
            int intervalDays,
            Instant nextDueAt,
            int xpAwarded,
            /** True when this review earned nothing because it had already been paid for. */
            boolean alreadyEarned) {
    }
}