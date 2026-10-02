package com.patternrun.progress;

import com.patternrun.account.UserEntity;
import com.patternrun.progress.dto.PatternMasteryResponse;
import com.patternrun.progress.dto.ProblemProgressResponse;
import com.patternrun.progress.dto.ProgressSummaryResponse;
import com.patternrun.progress.dto.ReviewItemResponse;
import com.patternrun.progress.dto.StreakResponse;
import com.patternrun.security.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The progress endpoints (README section 49).
 *
 * {@code /summary} exists so the dashboard is one request rather than six, and
 * {@code /problems} keeps per-learner state out of the content endpoints so those stay
 * user independent.
 */
@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final ProgressService progress;

    public ProgressController(ProgressService progress) {
        this.progress = progress;
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
}
