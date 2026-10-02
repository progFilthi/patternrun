package com.patternrun.mistake;

import com.patternrun.attempt.AttemptEntity;
import com.patternrun.execution.ExecutionOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Works out what a learner actually got wrong, from the attempt as the server recorded it.
 *
 * <h3>Why this exists</h3>
 *
 * The mistake journal, the review queue, the retention axis of mastery and the comeback award were
 * all built and all unreachable, because nothing ever recorded a mistake. This is the missing piece,
 * and it is deliberately a pure function of an attempt rather than a service: the classification is
 * a judgement about a set of signals, so it can be tested exhaustively without a database, and it
 * cannot quietly acquire a dependency or a transaction.
 *
 * <h3>What a mistake is</h3>
 *
 * Something the learner got wrong that a later attempt could get right. That rules out a great deal:
 * a wrong pattern guess is recorded as one mistake, not three, and a slow-but-correct session is not
 * a mistake at all.
 *
 * <h3>Order matters</h3>
 *
 * The most specific thing wins, and the list is capped. A learner who missed the pattern *and* the
 * code and the complexity has one thing wrong --- they did not see what the problem was --- and
 * listing three would triple their review queue for a single misunderstanding. The first match is
 * therefore returned, most specific first.
 */
public final class MistakeClassifier {

    /** The most mistakes one attempt may produce. */
    public static final int MAX_PER_ATTEMPT = 1;

    private MistakeClassifier() {
    }

    /** One recorded misunderstanding. */
    public record Finding(MistakeCategory category, String description, String lesson) {
    }

    /**
     * What this attempt got wrong, most significant first.
     *
     * Empty when the attempt was correct throughout, which is the common case and the reason this
     * returns a list rather than a nullable.
     */
    public static List<Finding> inspect(AttemptEntity attempt) {
        List<Finding> findings = new ArrayList<>();

        ExecutionOutcome code = attempt.getCodeOutcome();

        // Most specific first, because "your code threw" is a far more useful thing to record than
        // "you also missed the pattern" when both happened.

        // A crash is not a reasoning error. Recording it as LOGIC would send the learner to
        // re-derive an algorithm that was fine and send them away from a type error.
        if (code == ExecutionOutcome.RUNTIME_ERROR || code == ExecutionOutcome.SYNTAX_ERROR) {
            findings.add(new Finding(MistakeCategory.TESTS,
                    "The solution did not run: " + describeOutcome(code) + ".",
                    "Reproduce it on the visible examples first and read the error before changing "
                            + "the algorithm. A crash is a typo or a type, not a wrong idea."));
        }

        // A timeout is the one failure that is definitely about complexity rather than logic, so it
        // is classified as such even though the learner may also have got the answer wrong.
        if (code == ExecutionOutcome.TIME_LIMIT_EXCEEDED || code == ExecutionOutcome.MEMORY_LIMIT_EXCEEDED) {
            findings.add(new Finding(MistakeCategory.COMPLEXITY,
                    "The solution ran out of " + (code == ExecutionOutcome.MEMORY_LIMIT_EXCEEDED
                            ? "memory" : "time") + ".",
                    "Count how many times the work is repeated. Nested full scans are quadratic, and "
                            + "the solution below is linear."));
        }

        // Code that runs and returns the wrong thing is the genuine logic case.
        if (code == ExecutionOutcome.WRONG_ANSWER) {
            findings.add(new Finding(MistakeCategory.LOGIC,
                    "The solution ran but returned the wrong answer.",
                    "Find the smallest case that fails and work backwards from what it needs to be "
                            + "true. Check what you store before you move to the next element."));
        }

        // The pattern was misidentified. This comes before the softer signals because it explains
        // them: a learner who picked the wrong pattern tends to get the pseudocode and the
        // complexity wrong as a consequence, and recording all three would be three entries for one
        // mistake.
        if (!attempt.isPatternCorrect()) {
            findings.add(new Finding(MistakeCategory.PATTERN,
                    "Identified the pattern as " + attempt.getPatternGuess() + ", which is not this "
                            + "problem's.",
                    "Work from the constraints rather than the shape of the input. Ask what has to be "
                            + "remembered while scanning, and why the brute force is too slow."));
        } else if (!attempt.isComplexityCorrect()) {
            // Only meaningful once the pattern was right: otherwise the complexity is a consequence
            // of the wrong pattern and says nothing about this learner.
            findings.add(new Finding(MistakeCategory.COMPLEXITY,
                    "Identified the pattern correctly but miscounted the complexity.",
                    "Count the passes, not the lines. One pass over the array is linear; two nested "
                            + "passes are quadratic."));
        } else if (!attempt.isBreakdownCorrect()) {
            findings.add(new Finding(MistakeCategory.EXPLANATION,
                    "Read the problem as asking for something other than what it asked for.",
                    "Say out loud what you are given and what you must return before choosing a "
                            + "approach. Returning values instead of indices is the usual confusion."));
        }

        return findings.size() > MAX_PER_ATTEMPT ? findings.subList(0, MAX_PER_ATTEMPT) : findings;
    }

    /** A learner's own wording, or a safe default when the request recorded nothing. */
    public static String describeOutcome(ExecutionOutcome outcome) {
        if (outcome == null) {
            return "it did not run";
        }
        return switch (outcome) {
            case SYNTAX_ERROR -> "a syntax error stopped it before it ran";
            case RUNTIME_ERROR -> "it raised an error";
            case TIME_LIMIT_EXCEEDED -> "it did not finish in time";
            case MEMORY_LIMIT_EXCEEDED -> "it used more memory than allowed";
            case WRONG_ANSWER -> "it returned the wrong answer";
            case INTERNAL_ERROR -> "the runner could not evaluate it";
            case ACCEPTED -> "it passed";
        };
    }

    /** Convenience for callers that only want the first finding. */
    public static Optional<Finding> primary(AttemptEntity attempt) {
        return inspect(attempt).stream().findFirst();
    }
}