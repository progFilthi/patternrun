"use client"

import Link from "next/link"

import type { ProblemSummary } from "@/types/api"
import { DifficultyBadge } from "@/components/problems/difficulty-badge"
import { useCompletedSlugs } from "@/lib/training/progress-store"

export function ProblemList({ problems }: { problems: ProblemSummary[] }) {
  const completed = useCompletedSlugs()

  return (
    <ul className="divide-y">
      {problems.map((problem) => (
        <li key={problem.id}>
          <Link
            href={`/problems/${problem.slug}`}
            className="flex flex-col gap-2 py-4 transition-colors hover:bg-secondary/30 sm:flex-row sm:items-center sm:gap-6"
          >
            <span className="w-10 shrink-0 font-mono text-xs text-muted-foreground">
              {completed.has(problem.slug) ? (
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
      ))}
    </ul>
  )
}