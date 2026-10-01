"use client"

import Link from "next/link"

import type { ProblemDetail, ProblemSummary } from "@/types/api"
import { Button } from "@/components/ui/button"

/** COMPLETE: what you just did, and where to go next. No XP maths yet (Phase 3). */
export function CompletionSummary({
  problem,
  patternCorrect,
  hintsUsed,
  complexityCorrect,
  predictionsAnswered,
  nextProblem,
}: {
  problem: ProblemDetail
  patternCorrect: boolean
  hintsUsed: number
  complexityCorrect: boolean
  predictionsAnswered: number
  nextProblem?: ProblemSummary
}) {
  const checks = [
    { label: "Pattern identified", done: patternCorrect },
    { label: "Walkthrough finished", done: true },
    { label: "Predictions made", done: predictionsAnswered > 0, detail: String(predictionsAnswered) },
    { label: "Complexity confirmed", done: complexityCorrect },
    { label: "Hints used", done: hintsUsed <= 1, detail: `${hintsUsed} of 5` },
  ]

  return (
    <section aria-label="Session complete" className="flex flex-col gap-10">
      <header className="flex flex-col gap-3 border-b pb-6">
        <p className="font-mono text-[11px] uppercase tracking-[0.08em] text-success">complete</p>
        <h1 className="text-2xl font-medium tracking-tight">
          {problem.title} · {problem.pattern.name}
        </h1>
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
          {problem.interviewExplanation}
        </p>
      </header>

      <ul className="grid gap-2 sm:grid-cols-2">
        {checks.map((check) => (
          <li
            key={check.label}
            className="flex items-center justify-between rounded-md border px-4 py-3 text-sm"
          >
            <span className="flex items-center gap-3">
              <span aria-hidden className={check.done ? "text-success" : "text-muted-foreground"}>
                {check.done ? "✓" : "○"}
              </span>
              {check.label}
            </span>
            {check.detail && <span className="font-mono text-xs text-muted-foreground">{check.detail}</span>}
          </li>
        ))}
      </ul>

      <section aria-label="The invariant to remember" className="flex flex-col gap-2 border-t pt-8">
        <h2 className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
          Remember this
        </h2>
        <p className="max-w-3xl border-l-2 border-foreground/20 pl-4 text-[15px] leading-relaxed">
          {problem.invariant}
        </p>
      </section>

      <div className="flex flex-wrap items-center gap-3 border-t pt-8">
        {nextProblem ? (
          <Button asChild>
            <Link href={`/problems/${nextProblem.slug}`}>
              Next problem: {nextProblem.title}
            </Link>
          </Button>
        ) : (
          <Button asChild>
            <Link href="/problems">Back to problems</Link>
          </Button>
        )}
        <Button variant="outline" asChild>
          <Link href={`/patterns/${problem.pattern.slug}`}>Review {problem.pattern.name}</Link>
        </Button>
      </div>
    </section>
  )
}