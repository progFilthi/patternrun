"use client"

import { useCallback, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Check, Loader2, RotateCcw, X } from "lucide-react"

import { Button } from "@/components/ui/button"
import { reviewMistake } from "@/lib/api/session"
import type { ReviewItem, ReviewOutcome } from "@/types/api"

/**
 * The review queue.
 *
 * Deliberately the only interactive part of the progress screen. Everything else is a number the
 * server computed, and a number that needed a round trip to display would be a number the client
 * could also have invented.
 *
 * <h3>Why the learner answers this themselves</h3>
 *
 * "Do you have this now?" is a question about their own understanding. The backend cannot grade it
 * without becoming a second quiz, and letting the client claim "I got it right" would put a
 * correctness judgement under client control — which is the one thing this whole system is built to
 * avoid. So the answer is accepted, used only to decide when the mistake comes back, and the XP is
 * paid for coming back at all.
 */
export function ReviewQueue({ items }: { items: ReviewItem[] }) {
  const router = useRouter()
  const [busy, setBusy] = useState<null | "correct" | "wrong">(null)
  const [last, setLast] = useState<ReviewOutcome | null>(null)
  const [failure, setFailure] = useState<string | null>(null)

  /**
   * What has been dealt with in this sitting, rather than a copy of the queue.
   *
   * Storing the ids instead of an index or a copied item means the visible item is derived, so
   * there is no state that can disagree with the props. It also survives `router.refresh()`: the
   * server stops returning a reviewed mistake, and the ones already done are filtered out anyway,
   * so neither arrival nor refresh can shuffle the queue under the learner.
   */
  const [reviewed, setReviewed] = useState<ReadonlySet<string>>(() => new Set())
  const pending = items.filter((item) => !reviewed.has(item.mistakeId))
  const current = pending[0] ?? null

  const answer = useCallback(
    async (correct: boolean) => {
      if (!current || busy) return
      setBusy(correct ? "correct" : "wrong")
      setFailure(null)
      try {
        const outcome = await reviewMistake(current.mistakeId, correct)
        setLast(outcome)
        setReviewed((previous) => new Set(previous).add(current.mistakeId))
        // The award and the daily counters changed server-side, so anything derived from the
        // summary is now stale. Refetching beats guessing which numbers moved.
        router.refresh()
      } catch (error) {
        setFailure(
          error instanceof Error ? error.message : "That review did not go through. Try again.",
        )
      } finally {
        setBusy(null)
      }
    },
    [busy, current, router],
  )

  /**
   * What the server said this review was worth.
   *
   * Shared by both states on purpose. Emptying the queue is the moment the learner most wants to
   * know what they just earned, and an empty-state card that quietly dropped the award would hide
   * the one number they came for.
   */
  const outcomeLine = last ? (
    <p aria-live="polite" className="flex items-center gap-2 text-sm text-muted-foreground">
      <RotateCcw className="size-3.5" aria-hidden />
      {last.alreadyEarned ? "Reviewed. " : `Reviewed, +${last.xpAwarded} XP. `}
      Back in {last.intervalDays} day{last.intervalDays === 1 ? "" : "s"}.
    </p>
  ) : null

  if (!current) {
    return (
      <div className="space-y-4">
        <div className="rounded-md border border-dashed p-6 text-center">
          <p className="text-sm font-medium">Nothing due for review</p>
          {!last ? (
            <p className="mx-auto mt-1 max-w-md text-sm text-muted-foreground">
              When you get something wrong, it lands here when it is time to think about it again.
            </p>
          ) : null}
        </div>
        {outcomeLine}
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <article className="rounded-md border p-5">
        <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
          <span className="font-mono text-xs uppercase tracking-[0.08em] text-muted-foreground">
            {current.category}
          </span>
          <Link
            href={`/problems/${current.problemSlug}`}
            className="text-sm font-medium underline underline-offset-4 hover:text-foreground"
          >
            {current.problemTitle}
          </Link>
          {current.reviewCount > 0 ? (
            <span className="text-xs text-muted-foreground">
              seen {current.reviewCount} time{current.reviewCount === 1 ? "" : "s"}
            </span>
          ) : null}
        </div>

        <p className="mt-3 text-[15px] leading-relaxed">{current.description}</p>
        <p className="mt-2 border-l-2 border-l-foreground/20 pl-4 text-[15px] leading-relaxed text-muted-foreground">
          {current.lesson}
        </p>

        <div className="mt-5 flex flex-wrap items-center gap-2">
          <Button onClick={() => answer(true)} disabled={busy !== null}>
            {busy === "correct" ? (
              <Loader2 className="size-4 animate-spin" aria-hidden />
            ) : (
              <Check className="size-4" aria-hidden />
            )}
            I have this now
          </Button>
          <Button onClick={() => answer(false)} disabled={busy !== null} variant="outline">
            {busy === "wrong" ? (
              <Loader2 className="size-4 animate-spin" aria-hidden />
            ) : (
              <X className="size-4" aria-hidden />
            )}
            Still not
          </Button>
          <p className="text-xs text-muted-foreground">
            Both answer honestly. Only coming back is rewarded.
          </p>
        </div>

        {failure ? (
          <p role="alert" className="mt-3 text-sm text-warning">
            {failure}
          </p>
        ) : null}
      </article>

      {outcomeLine}

      {pending.length > 1 ? (
        <p className="text-xs text-muted-foreground">
          {pending.length} due in total. This is the first.
        </p>
      ) : null}
    </div>
  )
}