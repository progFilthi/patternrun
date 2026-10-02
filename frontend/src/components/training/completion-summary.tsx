"use client"

import Link from "next/link"
import { AlertTriangle, Loader2, RotateCcw } from "lucide-react"

import type { CompletionResult, ProblemDetail, ProblemSummary } from "@/types/api"
import { Button } from "@/components/ui/button"
import type { SessionState } from "@/lib/training/use-training-attempt"

/**
 * COMPLETE: what the backend decided this session was worth.
 *
 * Every number here arrives in {@link CompletionResult} and is rendered as-is. This component
 * contains no XP maths, no combo arithmetic, no grade calculation and no inference about whether
 * anything was correct, because the server has already decided all of it and a second opinion
 * here could only disagree with the ledger.
 *
 * The tone is deliberately restrained. A reward that shouts is a reward that gets tuned out, so
 * the total is large but not animated, the awards are a quiet list, and nothing on this screen
 * competes with the invariant below it.
 */

export function CompletionSummary({
  problem,
  state,
  onRetry,
  nextProblem,
}: {
  problem: ProblemDetail
  state: SessionState
  onRetry: () => void
  nextProblem?: ProblemSummary
}) {
  if (state.phase === "completing" || state.phase === "submitting-action") {
    return (
      <CompletionShell problem={problem}>
        {/* A live region, so the change of state is announced rather than only animated. */}
        <div role="status" className="flex items-center gap-3 text-[15px] text-muted-foreground">
          <Loader2 aria-hidden className="size-4 animate-spin" />
          <span>Working out what this session was worth…</span>
        </div>
      </CompletionShell>
    )
  }

  if (state.phase === "failed") {
    return (
      <CompletionShell problem={problem}>
        <div className="flex flex-col gap-4" role="alert">
          <div className="flex items-start gap-3">
            <AlertTriangle aria-hidden className="mt-0.5 size-4 shrink-0 text-warning" />
            <div className="flex flex-col gap-1">
              <p className="text-[15px] leading-relaxed">
                {state.error ?? "The result could not be saved."}
              </p>
              {/* Explicitly not showing an XP total here: inventing one would be a lie, and
                  a learner who is told they earned nothing when they earned something is worse
                  off than one who is told to try again. */}
              <p className="text-sm text-muted-foreground">
                Your session is still saved. Retrying will not count it twice.
              </p>
            </div>
          </div>
          {state.retryable !== false && (
            <div>
              <Button variant="outline" onClick={onRetry}>
                <RotateCcw aria-hidden className="size-4" />
                Try again
              </Button>
            </div>
          )}
        </div>
      </CompletionShell>
    )
  }

  const result = state.result

  // The session finished but the backend has not answered yet, or never did. Say so rather
  // than showing an empty result that reads like "you earned nothing".
  if (!result) {
    return (
      <CompletionShell problem={problem}>
        <div className="flex flex-col gap-4">
          <p className="text-[15px] leading-relaxed text-muted-foreground">
            The session is finished, but its result has not come back yet.
          </p>
          <div>
            <Button variant="outline" onClick={onRetry}>
              <RotateCcw aria-hidden className="size-4" />
              Check again
            </Button>
          </div>
        </div>
      </CompletionShell>
    )
  }

  return (
    <CompletionShell problem={problem}>
      <RewardPanel result={result} />

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
    </CompletionShell>
  )
}

/** The total, the run multiplier, and the individual awards the backend recorded. */
function RewardPanel({ result }: { result: CompletionResult }) {
  const repeat = result.totalXpAwarded === 0

  return (
    <section aria-label="What this session earned" className="flex flex-col gap-5">
      <div className="flex flex-wrap items-baseline gap-x-4 gap-y-2">
        <p className="text-3xl font-medium tracking-tight tabular-nums">
          {repeat ? "No new XP" : `+${result.totalXpAwarded} XP`}
        </p>
        {result.combo > 1 && (
          <p className="font-mono text-xs uppercase tracking-[0.08em] text-muted-foreground">
            Combo ×{result.combo}
          </p>
        )}
        <p className="font-mono text-xs uppercase tracking-[0.08em] text-muted-foreground">
          {result.grade.replaceAll("_", " ")}
          {result.provisional ? " · provisional" : ""}
        </p>
      </div>

      {/* Repeats say why they earned nothing, rather than looking like a bug. */}
      {repeat && (
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
          You have already been paid for this one. Solving it again still sharpens your mastery
          and it still counts towards your streak.
        </p>
      )}

      {result.awards.length > 0 && (
        <ul className="grid gap-x-8 gap-y-2 sm:grid-cols-2">
          {result.awards.map((award) => (
            <li key={`${award.reason}-${award.xp}`} className="flex items-baseline justify-between gap-4">
              <span className="flex items-center gap-2 text-sm">
                <span aria-hidden className="text-success">
                  ✓
                </span>
                {award.label}
              </span>
              <span className="font-mono text-xs tabular-nums text-muted-foreground">
                +{award.xp}
              </span>
            </li>
          ))}
        </ul>
      )}

      {result.patternMastery !== undefined && (
        <div className="flex flex-col gap-2 border-t pt-5">
          <p className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
            {result.patternSlug} mastery
          </p>
          <div className="flex items-center gap-3">
            <MasteryBar value={result.patternMastery} />
            <span className="font-mono text-sm tabular-nums">
              {Math.round(result.patternMastery)}%
            </span>
          </div>
        </div>
      )}
    </section>
  )
}

function MasteryBar({ value }: { value: number }) {
  const clamped = Math.max(0, Math.min(100, value))
  return (
    <div
      role="meter"
      aria-valuenow={Math.round(clamped)}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label="Pattern mastery"
      className="h-1.5 w-full max-w-xs overflow-hidden rounded-full bg-muted"
    >
      <div
        className="h-full rounded-full bg-foreground transition-[width] duration-500"
        style={{ width: `${clamped}%` }}
      />
    </div>
  )
}

function CompletionShell({
  problem,
  children,
}: {
  problem: ProblemDetail
  children: React.ReactNode
}) {
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
      {children}
    </section>
  )
}