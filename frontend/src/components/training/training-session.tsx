"use client"

import { useCallback, useMemo, useState } from "react"

import type { AnimationStep, Hint, PatternSummary, ProblemDetail, ProblemSummary } from "@/types/api"
import { AnimationStage } from "@/components/animation/animation-stage"
import { Button } from "@/components/ui/button"
import { ProblemBrief } from "@/components/training/problem-brief"
import { PatternGuess } from "@/components/training/pattern-guess"
import { HintLadder } from "@/components/training/hint-ladder"
import { ComplexityCheck } from "@/components/training/complexity-check"
import { ExplanationPanel } from "@/components/training/explanation-panel"
import { CompletionSummary } from "@/components/training/completion-summary"
import { SessionProgress } from "@/components/training/session-progress"
import { COMPLEXITY_OPTIONS, isCorrectChoice } from "@/lib/training/complexity"
import { saveCompletion } from "@/lib/training/progress-store"

/** The training loop (README section 60). One step at a time, no phase skipping. */
const PHASES = [
  { id: "SCOUT", label: "Read" },
  { id: "PATTERN_GUESS", label: "Identify" },
  { id: "ANIMATION", label: "Animate" },
  { id: "HINTS", label: "Hint" },
  { id: "EXPLANATION", label: "Explain" },
  { id: "COMPLEXITY", label: "Complexity" },
  { id: "COMPLETE", label: "Finish" },
] as const

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
  /** Other problems of the same pattern, used for the "next level" hand off. */
  siblings: ProblemSummary[]
}) {
  const [phaseIndex, setPhaseIndex] = useState(0)
  const [furthestPhase, setFurthestPhase] = useState(1)
  const [patternChoice, setPatternChoice] = useState<string | null>(null)
  const [patternConfirmed, setPatternConfirmed] = useState(false)
  const [animationFinished, setAnimationFinished] = useState(false)
  const [highestHint, setHighestHint] = useState(0)
  const [timeChoice, setTimeChoice] = useState<string | null>(null)
  const [spaceChoice, setSpaceChoice] = useState<string | null>(null)
  const [complexityChecked, setComplexityChecked] = useState(false)
  const [predictions, setPredictions] = useState(0)
  const [recorded, setRecorded] = useState(false)

  const phase = PHASES[phaseIndex].id

  const patternCorrect = useMemo(
    () => patternChoice === problem.pattern.slug,
    [patternChoice, problem.pattern.slug],
  )

  const complexityCorrect = useMemo(
    () =>
      isCorrectChoice(timeChoice, problem.complexity.time) &&
      isCorrectChoice(spaceChoice, problem.complexity.space),
    [problem.complexity.space, problem.complexity.time, spaceChoice, timeChoice],
  )

  /**
 * Phases only unlock by passing their gate, because the only way forward is the Continue
 * button, which stays disabled until the gate is met. Tracking the furthest phase reached
 * therefore doubles as the record of what the learner has earned: the rail can offer those
 * phases as links back, and nothing more.
 */
const goToPhase = useCallback((index: number) => {
    const clamped = Math.min(Math.max(index, 0), PHASES.length - 1)
    setPhaseIndex(clamped)
    setFurthestPhase((current) => Math.max(current, clamped))
  }, [])

  const finish = useCallback(() => {
    if (recorded) return
    saveCompletion(problem.slug, {
      completedAt: new Date().toISOString(),
      patternCorrect,
      hintsUsed: highestHint,
      complexityCorrect,
      predictionsAnswered: predictions,
    })
    setRecorded(true)
    goToPhase(PHASES.length - 1)
  }, [
    complexityCorrect,
    goToPhase,
    highestHint,
    patternCorrect,
    predictions,
    problem.slug,
    recorded,
  ])

  return (
    <div className="mx-auto w-full max-w-5xl px-6 pb-24 pt-8">
      <SessionProgress
        phases={PHASES}
        currentIndex={phaseIndex}
        reachableIndex={furthestPhase}
        onSelect={goToPhase}
      />

      <div className="mt-10">
        {phase === "SCOUT" && (
          <section aria-label="Step 1: read the problem" className="flex flex-col gap-8">
            <ProblemBrief problem={problem} />
            <div className="flex justify-end">
              <Button onClick={() => goToPhase(1)}>Start training</Button>
            </div>
          </section>
        )}

        {phase === "PATTERN_GUESS" && (
          <PatternGuess
            problem={problem}
            patterns={patterns}
            choice={patternChoice}
            confirmed={patternConfirmed}
            onSelect={setPatternChoice}
            onConfirm={() => setPatternConfirmed(true)}
            onContinue={() => goToPhase(2)}
          />
        )}

        {phase === "ANIMATION" && (
          <section aria-label="Step 3: animated walkthrough" className="flex flex-col gap-6">
            <AnimationStage
              steps={steps}
              onStepChange={(index) => setAnimationFinished(index >= steps.length - 1)}
              onPrediction={() => setPredictions((current) => current + 1)}
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
            highestRevealed={highestHint}
            onReveal={(level) => setHighestHint(Math.max(highestHint, level))}
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
            correct={complexityCorrect}
            onTimeChange={setTimeChoice}
            onSpaceChange={setSpaceChoice}
            onCheck={() => setComplexityChecked(true)}
            onContinue={finish}
          />
        )}

        {phase === "COMPLETE" && (
          <CompletionSummary
            problem={problem}
            patternCorrect={patternCorrect}
            hintsUsed={highestHint}
            complexityCorrect={complexityCorrect}
            predictionsAnswered={predictions}
            nextProblem={siblings.find((sibling) => sibling.slug !== problem.slug)}
          />
        )}
      </div>
    </div>
  )
}