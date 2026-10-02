import Link from "next/link"

import type { ProblemSummary } from "@/types/api"
import { DifficultyBadge } from "@/components/problems/difficulty-badge"

/**
 * The problem library.
 *
 * Completion state arrives as a prop from the server rather than from local storage, because
 * the backend is the record of what has been trained. Reading it here also removes the old
 * hydration gap where the tick only appeared after the page had loaded.
 */
export function ProblemList({
  problems,
  completedSlugs = new Set<string>(),
}: {
  problems: ProblemSummary[]
  completedSlugs?: ReadonlySet<string>
}) {
  return (
    <ul className="divide-y">
      {problems.map((problem) => {
        const completed = completedSlugs.has(problem.slug)
        return (
          <li key={problem.id}>
            <Link
              href={`/problems/${problem.slug}`}
              className="flex flex-col gap-2 py-4 transition-colors hover:bg-secondary/30 sm:flex-row sm:items-center sm:gap-6"
            >
              <span className="w-10 shrink-0 font-mono text-xs text-muted-foreground">
                {completed ? (
                  // Not colour alone: the shape differs and the list label says what it means.
                  <span className="text-success" title="Completed">
                    ✓
                  </span>
                ) : (
                  `#${problem.externalId}`
                )}
              </span>
              <span className="flex-1 text-[15px]">{problem.title}</span>
              <span className="flex shrink-0 items-center gap-3">
                <DifficultyBadge difficulty={problem.difficulty} />
                <span className="w-24 text-right font-mono text-xs text-muted-foreground">
                  {problem.complexity.time}
                </span>
              </span>
            </Link>
          </li>
        )
      })}
    </ul>
  )
}