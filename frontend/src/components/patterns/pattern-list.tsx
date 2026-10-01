import Link from "next/link"

import type { PatternSummary } from "@/types/api"

/** Pattern picker, ordered by the learning path (README section 122). */
export function PatternList({ patterns }: { patterns: PatternSummary[] }) {
  return (
    <ul className="divide-y">
      {patterns.map((pattern) => (
        <li key={pattern.id}>
          <Link
            href={`/patterns/${pattern.slug}`}
            className="group flex flex-col gap-1.5 py-5 transition-colors hover:bg-secondary/30 sm:flex-row sm:items-baseline sm:gap-8"
          >
            <span className="w-8 shrink-0 font-mono text-xs text-muted-foreground">
              {String(pattern.difficultyOrder).padStart(2, "0")}
            </span>
            <span className="w-44 shrink-0 text-[15px] font-medium">{pattern.name}</span>
            <span className="flex-1 text-sm leading-relaxed text-muted-foreground">
              {pattern.signal}
            </span>
            <span className="shrink-0 font-mono text-xs text-muted-foreground">
              {pattern.problemCount} {pattern.problemCount === 1 ? "problem" : "problems"}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}