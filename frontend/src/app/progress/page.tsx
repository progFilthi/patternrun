import Link from "next/link"

import { PageHeader, Section } from "@/components/layout/page-header"
import { ReviewQueue } from "@/components/progress/review-queue"
import { getProgressSummary, getReviewQueue } from "@/lib/api/progress"

/**
 * What the learner has actually achieved.
 *
 * Everything on this page is computed by the backend from the attempt history. The server component
 * reads it in one request and forwards the caller's cookie, so what is rendered is what the API
 * says and not a second opinion formed in the browser.
 *
 * This page is the reason the progress endpoints exist. They were built in Phase 3, returned
 * correct numbers, and were displayed nowhere — the dashboard still said "progress figures arrive
 * with the game layer". Backend authority without a surface for it is authority nobody can see.
 */
export const dynamic = "force-dynamic"

export default async function ProgressPage() {
  const [summary, review] = await Promise.all([getProgressSummary(), getReviewQueue()])

  if (!summary) {
    // A progress page that cannot reach the API should say so rather than render zeroes, which
    // would read as "you have achieved nothing" rather than "we could not ask".
    return (
      <div className="mx-auto w-full max-w-6xl px-6 pb-24 pt-14">
        <PageHeader
          eyebrow="progress"
          title="Your progress"
          lede="The API is not reachable right now, so there is nothing to show. Your records are safe."
        />
      </div>
    )
  }

  const trained = summary.problemsCompleted > 0

  return (
    <div className="mx-auto w-full max-w-6xl px-6 pb-24 pt-14">
      <PageHeader
        eyebrow="progress"
        title={trained ? `${summary.levelName}, level ${summary.level}` : "No sessions yet"}
        lede={
          trained
            ? "Everything here is worked out from your attempt history on the server."
            : "Finish one problem and this page starts answering: what you have trained, what you are strong at, and what is worth going back to."
        }
      />

      <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Total XP" value={summary.totalXp.toLocaleString()} />
        <Stat
          label="Level progress"
          value={`${Math.round(summary.levelProgress)}%`}
          detail={trained ? `level ${summary.level} of the curve` : undefined}
        />
        <Stat
          label="Problems solved"
          value={String(summary.problemsCompleted)}
          detail={summary.openMistakes > 0 ? `${summary.openMistakes} to review` : undefined}
        />
        <Stat
          label="Streak"
          value={`${summary.streak} day${summary.streak === 1 ? "" : "s"}`}
          detail={`longest ${summary.longestStreak}`}
        />
      </div>

      {summary.today ? (
        <Section className="mt-14" title="Today">
          <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
            <Figure label="XP earned" value={summary.today.xpEarned} />
            <Figure label="Problems solved" value={summary.today.problemsCompleted} />
            <Figure label="Mistakes reviewed" value={summary.today.mistakesReviewed} />
            <Figure
              label="Daily goal"
              value={summary.today.goalMet ? "met" : `${summary.today.activeMinutes} min`}
            />
          </dl>
        </Section>
      ) : null}

      <Section
        className="mt-14"
        title="What to think about"
        lede="A mistake comes back when it is time to. Answering both ways honestly is the point; only coming back is rewarded."
      >
        <ReviewQueue items={review} />
      </Section>

      <Section
        className="mt-14"
        title="Pattern mastery"
        lede="Measured from your completed sessions. A dash means the axis has not been measured yet, which is not the same as zero."
      >
        {summary.patterns.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            Mastery appears once you have completed a problem.
          </p>
        ) : (
          <ul className="divide-y rounded-md border">
            {summary.patterns.map((pattern) => (
              <li key={pattern.patternSlug} className="flex flex-col gap-2 p-4 sm:flex-row sm:items-center sm:gap-6">
                <div className="flex-1">
                  <Link
                    href={`/patterns/${pattern.patternSlug}`}
                    className="text-sm font-medium underline underline-offset-4 hover:text-foreground"
                  >
                    {pattern.patternName}
                  </Link>
                  <p className="text-xs text-muted-foreground">
                    {pattern.attemptsCount === 0
                      ? "not started"
                      : `${pattern.attemptsCount} session${pattern.attemptsCount === 1 ? "" : "s"}`}
                  </p>
                </div>
                <Meter label="Recognition" value={pattern.recognition} />
                <Meter label="Correctness" value={pattern.correctness} />
                <Meter label="Explanation" value={pattern.explanation} />
                <Meter label="Overall" value={pattern.overall} />
              </li>
            ))}
          </ul>
        )}
      </Section>
    </div>
  )
}

/**
 * One headline number.
 *
 * A dash is rendered rather than a zero for an unmeasured value, because "you have 0% recognition"
 * and "recognition has not been measured" are different statements and only one of them is true of
 * a learner who has never attempted the pattern.
 */
function Stat({
  label,
  value,
  detail,
}: {
  label: string
  value: string
  detail?: string
}) {
  return (
    <div className="rounded-md border p-5">
      <p className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">{label}</p>
      <p className="mt-2 text-2xl font-medium tracking-tight tabular-nums">{value}</p>
      {detail ? <p className="mt-1 text-xs text-muted-foreground">{detail}</p> : null}
    </div>
  )
}

function Figure({ label, value }: { label: string; value: string | number }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1 text-lg font-medium tabular-nums">{value}</dd>
    </div>
  )
}

/**
 * One mastery axis.
 *
 * A dash rather than a bar when the axis was never measured, which is what the API returns as an
 * absent field. Rendering that as 0% would tell a learner they are bad at something they have not
 * attempted.
 */
function Meter({ label, value }: { label: string; value?: number }) {
  const measured = value !== undefined
  const width = measured ? Math.max(0, Math.min(100, value)) : 0

  return (
    <div className="w-full sm:w-28">
      <div className="flex items-baseline justify-between gap-2">
        <span className="text-xs text-muted-foreground">{label}</span>
        <span className="font-mono text-xs tabular-nums text-muted-foreground">
          {measured ? `${Math.round(value as number)}%` : "—"}
        </span>
      </div>
      <div
        className="mt-1 h-1.5 rounded-full bg-muted"
        role="meter"
        aria-valuenow={measured ? Math.round(value as number) : undefined}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={label}
      >
        <div
          className={`h-full rounded-full transition-[width] duration-500 ${
            measured ? "bg-foreground" : "bg-transparent"
          }`}
          style={{ width: `${width}%` }}
        />
      </div>
    </div>
  )
}