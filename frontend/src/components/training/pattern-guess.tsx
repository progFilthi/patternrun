"use client"

import type { PatternSummary, ProblemDetail } from "@/types/api"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/**
 * PATTERN_GUESS: recognition before implementation (README section 5). The correct answer is
 * only revealed after a choice, so the habit is trained instead of the answer.
 */
export function PatternGuess({
  problem,
  patterns,
  choice,
  confirmed,
  onSelect,
  onConfirm,
  onContinue,
}: {
  problem: ProblemDetail
  patterns: PatternSummary[]
  choice: string | null
  confirmed: boolean
  onSelect: (slug: string) => void
  onConfirm: () => void
  onContinue: () => void
}) {
  const correct = choice === problem.pattern.slug

  return (
    <section aria-label="Step 2: identify the pattern" className="flex flex-col gap-8">
      <header className="flex flex-col gap-3 border-b pb-6">
        <p className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          step 2 · identify
        </p>
        <h1 className="text-2xl font-medium tracking-tight">Which pattern would you try first?</h1>
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
          Read the constraints again. Ask what is changing, then commit to one pattern. No code yet.
        </p>
      </header>

      <div
        role="radiogroup"
        aria-label="Pattern choice"
        className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3"
      >
        {patterns.map((pattern) => {
          const selected = choice === pattern.slug
          const reveal = confirmed && selected
          const revealMiss = confirmed && !selected && pattern.slug === problem.pattern.slug
          return (
            <button
              key={pattern.slug}
              type="button"
              role="radio"
              aria-checked={selected}
              disabled={confirmed}
              onClick={() => onSelect(pattern.slug)}
              className={cn(
                "rounded-md border px-4 py-3 text-left transition-colors disabled:cursor-default",
                reveal && "border-success",
                revealMiss && "border-foreground",
                !reveal && !revealMiss && (selected ? "border-foreground" : "border-border hover:border-foreground/40"),
              )}
            >
              <span className="block text-sm font-medium">{pattern.name}</span>
              <span className="mt-1 block text-xs leading-relaxed text-muted-foreground">
                {pattern.signal}
              </span>
            </button>
          )
        })}
      </div>

      {confirmed && (
        <div className="flex flex-col gap-3 border-t pt-6">
          <p className={cn("text-sm", correct ? "text-success" : "text-danger")}>
            {correct ? "Correct read of the signal." : `The signal points at ${problem.pattern.name}.`}
          </p>
          <div className="flex flex-col gap-2">
            <p className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
              why this pattern
            </p>
            <p className="max-w-2xl text-[15px] leading-relaxed">{problem.whyThisPattern}</p>
          </div>
          <div className="flex justify-end">
            <Button onClick={onContinue}>Continue to the walkthrough</Button>
          </div>
        </div>
      )}

      {!confirmed && (
        <div className="flex justify-end border-t pt-6">
          <Button onClick={onConfirm} disabled={!choice}>
            Lock in my answer
          </Button>
        </div>
      )}
    </section>
  )
}