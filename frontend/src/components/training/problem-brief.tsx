import { Badge } from "@/components/ui/badge"
import { DifficultyBadge } from "@/components/problems/difficulty-badge"
import { TrainingDifficultyBadge } from "@/components/problems/training-difficulty-badge"
import type { ProblemDetail } from "@/types/api"

/** SCOUT: title, difficulty, examples and constraints only. No code, no pattern. */
export function ProblemBrief({ problem }: { problem: ProblemDetail }) {
  return (
    <div className="flex flex-col gap-10">
      <header className="flex flex-col gap-3 border-b pb-6">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="font-mono">
            #{problem.externalId}
          </Badge>
          <DifficultyBadge difficulty={problem.difficulty} />
          <TrainingDifficultyBadge value={problem.trainingDifficulty} />
        </div>
        <h1 className="text-2xl font-medium tracking-tight">{problem.title}</h1>
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">{problem.statement}</p>
      </header>

      <section aria-label="Examples" className="flex flex-col gap-4">
        <h2 className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">Examples</h2>
        {problem.examples.map((example) => (
          <div key={example.ordinal} className="rounded-md border bg-surface p-4">
            <p className="font-mono text-sm">
              {example.input} <span className="text-muted-foreground">→</span> {example.output}
            </p>
            <p className="mt-2 text-sm text-muted-foreground">{example.explanation}</p>
          </div>
        ))}
      </section>

      <section aria-label="Constraints" className="flex flex-col gap-4">
        <h2 className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
          Constraints
        </h2>
        <ul className="grid gap-1.5 font-mono text-[13px] text-muted-foreground sm:grid-cols-2">
          {problem.constraints.map((constraint) => (
            <li key={constraint}>· {constraint}</li>
          ))}
        </ul>
      </section>
    </div>
  )
}