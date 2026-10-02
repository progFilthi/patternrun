import Link from "next/link"

import { api } from "@/lib/api/client"
import { Button } from "@/components/ui/button"
import { PatternList } from "@/components/patterns/pattern-list"
import { PageHeader, Section } from "@/components/layout/page-header"

/**
 * Dashboard: answer "what should I train now?" first, then show the pattern system.
 *
 * Progress lives on its own page rather than here. The dashboard is a way in from a cold start,
 * and a learner who already has history does not want a wall of numbers before they can reach a
 * problem --- they want /progress, which is one click away.
 */
export const dynamic = "force-dynamic"

export default async function DashboardPage() {
  const patterns = await api.listPatterns()
  const problems = await api.listProblems({ size: 100 })
  const firstProblems = patterns[0] ? await api.listPatternProblems(patterns[0].slug) : []

  return (
    <div className="mx-auto w-full max-w-6xl px-6 pb-24 pt-14">
      <PageHeader
        eyebrow="today's training"
        title="Stop memorizing solutions. Train the pattern instead."
        lede="Pick a pattern, read one problem, identify the signal, watch the algorithm move, predict the next move, then explain why the solution works."
        actions={
          <>
            <Button asChild size="lg">
              <Link href="/patterns">Start with a pattern</Link>
            </Button>
            <Button asChild size="lg" variant="outline">
              <Link href="/problems">Browse all problems</Link>
            </Button>
          </>
        }
      />

      <Section
        className="mt-20"
        title="The pattern system"
        meta={`${patterns.length} patterns · ${problems.totalElements} problems`}
      >
        <PatternList patterns={patterns} />
      </Section>

      <Section
        className="mt-20"
        title="Start here"
        action={
          patterns[0] ? (
            <Link
              href={`/patterns/${patterns[0].slug}`}
              className="text-xs text-muted-foreground underline underline-offset-4 transition-colors hover:text-foreground"
            >
              {patterns[0].name}
            </Link>
          ) : undefined
        }
      >
        <ul className="divide-y">
          {firstProblems.map((problem) => (
            <li key={problem.id}>
              <Link
                href={`/problems/${problem.slug}`}
                className="flex items-center justify-between gap-6 py-4 transition-colors hover:bg-secondary/40"
              >
                <span className="text-[15px]">{problem.title}</span>
                <span className="font-mono text-xs text-muted-foreground">{problem.complexity.time}</span>
              </Link>
            </li>
          ))}
        </ul>
      </Section>
    </div>
  )
}