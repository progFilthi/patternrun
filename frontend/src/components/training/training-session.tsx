"use client"

import { useCallback, useEffect, useRef, useState } from "react"

import type { AnimationStep, Hint, PatternSummary, ProblemDetail, ProblemSummary } from "@/types/api"
import { AnimationStage } from "@/components/animation/animation-stage"
import { Button } from "@/components/ui/button"
import { ProblemBrief } from "@/components/training/problem-brief"
import { ProblemBreakdown } from "@/components/training/problem-breakdown"
import { PatternGuess } from "@/components/training/pattern-guess"
import { HintLadder } from "@/components/training/hint-ladder"
import { ComplexityCheck } from "@/components/training/complexity-check"
import { ExplanationPanel } from "@/components/training/explanation-panel"
import { CompletionSummary } from "@/components/training/completion-summary"
import { SessionProgress } from "@/components/training/session-progress"
import { COMPLEXITY_OPTIONS, isCorrectChoice } from "@/lib/training/complexity"
import { LAST_PHASE, PHASES, clampPhaseIndex, reachThrough } from "@/lib/training/phases"
import { useTrainingAttempt } from "@/lib/training/use-training-attempt"
import { useSession } from "@/components/layout/session-provider"

/**
 * The training session.
 *
 * Phase gating is unchanged and still local: the seven phases advance one at a time, and the
 * stepper still only offers what has been earned.
 *
 * What changed is where the record lives. Every learner action is sent to the backend as it
 * happens, and completion hands over to the server's verdict rather than to local arithmetic.
 * This component no longer writes progress to local storage, because the backend is the record
 * and a second copy would eventually disagree with it.
 */
export function TrainingSession({
  problem,
  patterns,
  hints,
  steps,
  siblings,
}: {
  problem: ProblemDetail
  patterns: PatternSummary[]
  hints: Hint[]
  steps: AnimationStep[]
  siblings: ProblemSummary[]
}) {
  const [phaseIndex, setPhaseIndex] = useState(0)
  const [furthestPhase, setFurthestPhase] = useState(1)
  const [animationFinished, setAnimationFinished] = useState(false)
  const [timeChoice, setTimeChoice] = useState<string | null>(null)
  const [spaceChoice, setSpaceChoice] = useState<string | null>(null)
  const [complexityChecked, setComplexityChecked] = useState(false)
  const [patternConfirmed, setPatternConfirmed] = useState(false)

  const session = useSession()
  const attempt = useTrainingAttempt(problem.slug)
  const { state } = attempt

  // When the session exists on the server, and it is a different problem from the one this
  // component was mounted for, the identity is settled and the loop can begin.
  const startedRef = useRef(false)
  useEffect(() => {
    if (session.status === "establishing" || startedRef.current) return
    if (state.attemptId) return
    startedRef.current = true
    void attempt.start()
  }, [session.status, state.attemptId, attempt])

  // Set on mount rather than in a useRef argument: an argument is evaluated on every render,
  // which is both impure and easy to misread as happening once.
  const startedAt = useRef<number>(0)
  useEffect(() => {
    startedAt.current = Date.now()
  }, [])

  const goToPhase = useCallback((index: number) => {
    const clamped = clampPhaseIndex(index)
    setPhaseIndex(clamped)
    setFurthestPhase((current) => reachThrough(current, clamped))
  }, [])

  const phase = PHASES[phaseIndex].id

  /**
 * Hands the session over to the backend.
 *
 * The payload is observations only: the complexity strings the learner picked and how long the
 * session took. There is deliberately no breakdown flag. Whether the problem was read correctly
 * was decided earlier, when the breakdown answers were submitted, and the server stored that
 * verdict itself.
 */
  const onComplete = useCallback(() => {
    void attempt
      .complete({
        complexityTime: timeChoice ?? "",
        complexitySpace: spaceChoice ?? "",
        durationMs: Math.max(1, Date.now() - startedAt.current),
      })
      // Advance whether the call succeeded or failed. The summary is where both outcomes are
      // shown: the backend's verdict, or the reason it never arrived with a way to retry. Staying
      // on the complexity step would leave a failure with nowhere to be seen.
      .finally(() => goToPhase(LAST_PHASE))
  }, [attempt, timeChoice, spaceChoice, goToPhase])

  const onRetryCompletion = useCallback(() => {
    void attempt.retry()
  }, [attempt])

  return (
    <div className="mx-auto w-full max-w-5xl px-6 pb-24 pt-8">
      <SessionProgress
        phases={PHASES}
        currentIndex={phaseIndex}
        /* Once the backend has finished with a session, the last phase is reachable. Derived
           here rather than pushed into state by an effect, which would be a second source of
           truth for something already implied by `state.phase`. */
        reachableIndex={state.phase === "complete" ? LAST_PHASE : furthestPhase}
        onSelect={goToPhase}
      />

      <div className="mt-10">
        {phase === "SCOUT" && (
          <section aria-label="Step 1: read the problem" className="flex flex-col gap-8">
            <ProblemBrief problem={problem} />

            {/* Reading before choosing, not after: the pattern choice depends on having
                established what the problem asks for. */}
            <ProblemBreakdown
              slug={problem.slug}
              submitted={state.breakdown}
              pending={state.breakdownPending}
              onSubmit={(answers) => void attempt.submitReading(answers)}
            />

            <div className="flex flex-wrap items-center justify-end gap-4">
              {state.error && state.phase === "failed" && (
                <p className="text-sm text-muted-foreground">{state.error}</p>
              )}
              <Button onClick={() => goToPhase(1)}>Start training</Button>
            </div>
          </section>
        )}

        {phase === "PATTERN_GUESS" && (
          <PatternGuess
            problem={problem}
            patterns={patterns}
            choice={state.patternChoice ?? null}
            confirmed={patternConfirmed || state.phase === "submitting-action"}
            onSelect={(slug) => {
              setPatternConfirmed(false)
              void attempt.choosePattern(slug)
            }}
            onConfirm={() => {
              // Confirm only. Advancing happens on the Continue button, because the point of
              // confirming is to stop and read whether the read of the signal was right.
              setPatternConfirmed(true)
            }}
            onContinue={() => goToPhase(2)}
          />
        )}

        {phase === "ANIMATION" && (
          <section aria-label="Step 3: animated walkthrough" className="flex flex-col gap-6">
            <AnimationStage
              steps={steps}
              onStepChange={(index) => setAnimationFinished(index >= steps.length - 1)}
              onPrediction={(stepOrder, chosenIndex) => {
                void attempt.predict(stepOrder, chosenIndex)
              }}
            />
            <div className="flex justify-end">
              <Button onClick={() => goToPhase(3)} disabled={!animationFinished}>
                Continue to hints
              </Button>
            </div>
          </section>
        )}

        {phase === "HINTS" && (
          <HintLadder
            hints={hints}
            highestRevealed={state.hintsUsed}
            onReveal={(level) => void attempt.revealHint(level)}
            onContinue={() => goToPhase(4)}
          />
        )}

        {phase === "EXPLANATION" && (
          <section aria-label="Step 5: pseudocode and explanation" className="flex flex-col gap-8">
            <ExplanationPanel problem={problem} />
            <div className="flex justify-end">
              <Button onClick={() => goToPhase(5)}>Continue to complexity</Button>
            </div>
          </section>
        )}

        {phase === "COMPLEXITY" && (
          <ComplexityCheck
            expected={problem.complexity}
            options={COMPLEXITY_OPTIONS}
            timeChoice={timeChoice}
            spaceChoice={spaceChoice}
            checked={complexityChecked}
            correct={
              isCorrectChoice(timeChoice, problem.complexity.time) &&
              isCorrectChoice(spaceChoice, problem.complexity.space)
            }
            onTimeChange={setTimeChoice}
            onSpaceChange={setSpaceChoice}
            onCheck={() => setComplexityChecked(true)}
            onContinue={onComplete}
          />
        )}

        {phase === "COMPLETE" && (
          <CompletionSummary
            problem={problem}
            state={state}
            onRetry={onRetryCompletion}
            nextProblem={siblings.find((sibling) => sibling.slug !== problem.slug)}
          />
        )}
      </div>
    </div>
  )
}