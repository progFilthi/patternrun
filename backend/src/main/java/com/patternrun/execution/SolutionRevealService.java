package com.patternrun.execution;

import com.patternrun.account.UserEntity;
import com.patternrun.attempt.AttemptEntity;
import com.patternrun.attempt.AttemptRepository;
import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.problem.ProblemSolutionEntity;
import com.patternrun.problem.ProblemSolutionRepository;
import com.patternrun.problem.ProgrammingLanguage;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The final escape hatch.
 *
 * Reaching the reference implementation is a legitimate way to finish a session. Someone who is
 * stuck has usually already taken the hints that reasoning could offer, and continuing to refuse
 * help teaches them to close the tab instead. What the product owes them is honesty about what
 * happened, so revealing the solution records that it was revealed — which is what lets review and
 * mastery tell independent solving from assisted solving later.
 *
 * <h2>Why it is not a read</h2>
 *
 * A {@code GET} on a solution would hand over the answer to anyone who guessed the URL, including a
 * learner who never opened an attempt, and it would leave no record that it happened. Making it a
 * POST against a live attempt means the answer can only be fetched by someone who is actually
 * working, and the fetch is itself the record.
 */
@Service
public class SolutionRevealService {

    private final AttemptRepository attempts;
    private final ProblemSolutionRepository solutions;

    public SolutionRevealService(AttemptRepository attempts, ProblemSolutionRepository solutions) {
        this.attempts = attempts;
        this.solutions = solutions;
    }

    @Transactional
    public RevealedSolution reveal(UserEntity user, UUID attemptId, ProgrammingLanguage language) {
        AttemptEntity attempt = attempts.requireLive(user.getId(), attemptId);
        ProblemSolutionEntity solution = solutions
                .findByProblemIdAndLanguage(attempt.getProblem().getId(), language)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No " + language + " reference solution for " + attempt.getProblem().getSlug()));

        // Recorded before the code is returned, in the same transaction. If the read succeeded the
        // flag is already true, so a learner can never obtain the answer without also being counted
        // as having had it.
        attempt.setSolutionRevealed(true);
        attempts.save(attempt);

        return new RevealedSolution(
                attempt.getProblem().getSlug(), language.name(), solution.getCode(), attempt.getHintsUsed());
    }

    public record RevealedSolution(
            String problemSlug,
            String language,
            String code,
            int hintsUsed) {
    }
}