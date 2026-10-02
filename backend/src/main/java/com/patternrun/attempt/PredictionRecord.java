package com.patternrun.attempt;

/**
 * One answer to a predict-the-move question (README section 8).
 *
 * The client reports which option was picked. `correct` is filled in by the server from the
 * animation step's own answerIndex, because the answer already travels to the browser inside
 * the step payload and a client that self-reports correctness is asserting something the server
 * can simply check.
 */
public record PredictionRecord(int stepOrder, int chosenIndex, boolean correct) {

    public PredictionRecord(int stepOrder, int chosenIndex) {
        this(stepOrder, chosenIndex, false);
    }
}
