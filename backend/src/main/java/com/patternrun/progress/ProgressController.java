package com.patternrun.progress;

import com.patternrun.account.UserEntity;
import com.patternrun.mistake.MistakeReviewService;
import com.patternrun.progress.dto.PatternMasteryResponse;
import com.patternrun.progress.dto.ProblemProgressResponse;
import com.patternrun.progress.dto.ProgressSummaryResponse;
import com.patternrun.progress.dto.ReviewItemResponse;
import com.patternrun.progress.dto.ReviewOutcomeResponse;
import com.patternrun.progress.dto.ReviewRequest;
import com.patternrun.progress.dto.StreakResponse;
import com.patternrun.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The progress endpoints (README section 49).
 *
 * {@code /summary} exists so the dashboard is one request rather than six, and
 * {@code /problems} keeps per-learner state out of the content endpoints so those stay
 * user independent.
 *
 * <p>Review is the one write here, and it exists because a queue nobody can act on is worse than an
 * empty one: it teaches a learner the number is not worth looking at. Everything else this
 * controller reports is derived, and derived state has nothing to write.
 */
@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final ProgressService progress;
    private final MistakeReviewService reviews;

    public ProgressController(ProgressService progress, MistakeReviewService reviews) {
        this.progress = progress;
        this.reviews = reviews;
    }

    @GetMapping
    public ProgressSummaryResponse summary(@CurrentUser UserEntity user) {
        return progress.summary(user);
    }

    @GetMapping("/patterns")
    public List<PatternMasteryResponse> patterns(@CurrentUser UserEntity user) {
        return progress.masteryList(user);
    }

    @GetMapping("/problems")
    public List<ProblemProgressResponse> problems(@CurrentUser UserEntity user) {
        return progress.problemStates(user);
    }

    @GetMapping("/streak")
    public StreakResponse streak(@CurrentUser UserEntity user) {
        return progress.streak(user);
    }

    @GetMapping("/daily")
    public ProgressSummaryResponse.DailyProgressResponse daily(@CurrentUser UserEntity user) {
        return progress.summary(user).today();
    }

    @GetMapping("/review")
    public List<ReviewItemResponse> review(@CurrentUser UserEntity user) {
        return progress.reviewQueue(user);
    }

    /**
     * Reviews one mistake and settles its next interval.
     *
     * The request carries the learner's own judgement of whether they now have it, and nothing else.
     * That is a claim about their understanding, which is not something the backend can verify, and
     * it is not treated as one: it decides the interval and nothing else. XP, the review count and
     * the next due date are all server-owned, and the award is keyed per mistake and review number
     * so a retried request cannot pay twice.
     */
    @PostMapping("/review/{mistakeId}")
    public ReviewOutcomeResponse reviewOne(
            @CurrentUser UserEntity user,
            @PathVariable UUID mistakeId,
            @Valid @RequestBody ReviewRequest request) {
        return ReviewOutcomeResponse.of(reviews.review(user, mistakeId, request.correct()));
    }
}
