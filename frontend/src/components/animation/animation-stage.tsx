"use client"

import { useCallback, useEffect, useMemo, useState, useSyncExternalStore } from "react"

import type { AnimationStep } from "@/types/api"
import { AnimationRenderer } from "@/components/animation/animation-renderer"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/** Base autoplay delay at 1x. README section 55 keeps transitions short and purposeful. */
const BASE_STEP_MS = 2600
const SPEEDS = [0.5, 1, 1.5, 2] as const

function isQuestion(step: AnimationStep | undefined): boolean {
  return step?.type === "QUESTION"
}

export function AnimationStage({
  steps,
  onStepChange,
  onPrediction,
}: {
  steps: AnimationStep[]
  /** Reported when the walkthrough reaches the last step, so the session can move on. */
  onStepChange?: (stepIndex: number) => void
  onPrediction?: (optionIndex: number, correct: boolean) => void
}) {
  const [index, setIndex] = useState(0)
  const [playing, setPlaying] = useState(false)
  const [speed, setSpeed] = useState<number>(1)
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const reducedMotion = usePrefersReducedMotion()

  const step = steps[index]
  const answered = answers[index]
  const locked = isQuestion(step) && answered === undefined

  const answerIndex = useMemo(() => {
    if (!step || step.type !== "QUESTION") return null
    const value = step.payload.answerIndex
    return typeof value === "number" ? value : -1
  }, [step])

  // Callbacks are invoked from the event handler, never from inside a state updater, so the
  // parent is never updated during this component's render.
  const goTo = useCallback(
    (next: number) => {
      const clamped = Math.min(Math.max(next, 0), Math.max(steps.length - 1, 0))
      setIndex(clamped)
      onStepChange?.(clamped)
    },
    [onStepChange, steps.length],
  )

  const advance = useCallback(() => {
    const next = Math.min(index + 1, steps.length - 1)
    setIndex(next)
    onStepChange?.(next)
  }, [index, onStepChange, steps.length])

  const handleAnswer = useCallback(
    (optionIndex: number) => {
      setAnswers((current) => ({ ...current, [index]: optionIndex }))
      onPrediction?.(optionIndex, optionIndex === answerIndex)
    },
    [answerIndex, index, onPrediction],
  )

  useEffect(() => {
    if (!playing || locked || index >= steps.length - 1) return
    const timer = window.setTimeout(advance, BASE_STEP_MS / speed)
    return () => window.clearTimeout(timer)
  }, [advance, index, locked, playing, speed, steps.length])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null
      if (target && ["INPUT", "TEXTAREA", "BUTTON"].includes(target.tagName)) return

      if (event.key === "ArrowRight") {
        event.preventDefault()
        if (!locked) advance()
      }
      if (event.key === "ArrowLeft") {
        event.preventDefault()
        goTo(index - 1)
      }
      if (event.key === " ") {
        event.preventDefault()
        if (!locked) setPlaying((current) => !current)
      }
    }
    window.addEventListener("keydown", onKeyDown)
    return () => window.removeEventListener("keydown", onKeyDown)
  }, [advance, goTo, index, locked])

  if (steps.length === 0) {
    return (
      <p className="rounded-md border border-dashed p-6 text-sm text-muted-foreground">
        This problem has no animated walkthrough yet.
      </p>
    )
  }

  return (
    <section aria-label="Animated walkthrough" className="flex flex-col gap-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
            step {step.order} of {steps.length} · {step.type}
          </p>
          <h3 className="mt-1 text-base font-medium">{step.title}</h3>
        </div>
        {isQuestion(step) && (
          <span
            className={cn(
              "shrink-0 rounded-md border px-2 py-1 text-[11px] font-medium",
              answered === undefined
                ? "border-foreground/30"
                : answered === answerIndex
                  ? "border-success/40 text-success"
                  : "border-danger/40 text-danger",
            )}
          >
            {answered === undefined ? "predict first" : answered === answerIndex ? "correct" : "recheck"}
          </span>
        )}
      </div>

      <AnimationRenderer
        type={step.type}
        payload={step.payload}
        context={{
          answer: answered === undefined ? null : answered,
          onAnswer: handleAnswer,
        }}
      />

      <p className="text-sm leading-relaxed text-muted-foreground">{step.description}</p>

      {/* Every animation carries a text alternative (README section 56). */}
      <p className="rounded-md border bg-surface px-3 py-2 text-xs leading-relaxed text-muted-foreground">
        {step.text}
      </p>

      <div className="flex items-center justify-between gap-4 border-t pt-4">
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => goTo(0)}
            disabled={index === 0}
            aria-label="Replay from the first step"
          >
            Replay
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => goTo(index - 1)}
            disabled={index === 0}
            aria-label="Previous step"
          >
            Previous
          </Button>
          <Button
            size="sm"
            onClick={() => {
              if (index >= steps.length - 1) {
                setPlaying(false)
                goTo(0)
                return
              }
              advance()
            }}
            disabled={locked}
            aria-label={index >= steps.length - 1 ? "Restart walkthrough" : "Next step"}
          >
            {index >= steps.length - 1 ? "Back to start" : "Next"}
          </Button>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setPlaying((current) => !current)}
            disabled={locked || reducedMotion || index >= steps.length - 1}
            title={reducedMotion ? "Autoplay is disabled because your system prefers reduced motion" : undefined}
          >
            {playing ? "Pause" : "Play"}
          </Button>
          <label className="flex items-center gap-1.5 text-xs text-muted-foreground">
            Speed
            <select
              value={speed}
              onChange={(event) => setSpeed(Number(event.target.value))}
              className="rounded-md border bg-background px-2 py-1 font-mono text-xs"
              aria-label="Playback speed"
            >
              {SPEEDS.map((option) => (
                <option key={option} value={option}>
                  {option}x
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="flex gap-1" aria-hidden>
        {steps.map((item, itemIndex) => (
          <button
            key={item.order}
            type="button"
            tabIndex={-1}
            onClick={() => goTo(itemIndex)}
            aria-label={`Go to step ${item.order}`}
            className={cn(
              "h-1 flex-1 rounded-full transition-colors",
              itemIndex === index ? "bg-foreground" : "bg-border hover:bg-muted-foreground/40",
            )}
          />
        ))}
      </div>
      <p className="text-xs text-muted-foreground">
        Keyboard: ← previous, → next, space play or pause.
      </p>
    </section>
  )
}

const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)"

function subscribeToReducedMotion(onChange: () => void): () => void {
  const query = window.matchMedia(REDUCED_MOTION_QUERY)
  query.addEventListener("change", onChange)
  return () => query.removeEventListener("change", onChange)
}

function readReducedMotion(): boolean {
  return window.matchMedia(REDUCED_MOTION_QUERY).matches
}

/** Autoplay stays available, but motion itself is already suppressed in globals.css. */
function usePrefersReducedMotion(): boolean {
  return useSyncExternalStore(subscribeToReducedMotion, readReducedMotion, () => false)
}