import { notFound } from "next/navigation"

import { api, ApiRequestError } from "@/lib/api/client"
import type { AnimationStep, Hint, PatternSummary, ProblemDetail, ProblemSummary } from "@/types/api"
import { TrainingSession } from "@/components/training/training-session"
import { Badge } from "@/components/ui/badge"
import { DifficultyBadge } from "@/components/problems/difficulty-badge"
import { Breadcrumb } from "@/components/layout/breadcrumb"

export const dynamic = "force-dynamic"

type TrainingData = {
  problem: ProblemDetail
  patterns: PatternSummary[]
  hints: Hint[]
  steps: AnimationStep[]
  siblings: ProblemSummary[]
}

async function loadTraining(slug: string): Promise<TrainingData | null> {
  try {
    const [problem, patterns, hints, steps] = await Promise.all([
      api.getProblem(slug),
      api.listPatterns(),
      api.getHints(slug),
      api.getAnimation(slug),
    ])
    const siblings = await api.listPatternProblems(problem.pattern.slug)
    return { problem, patterns, hints, steps, siblings }
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) return null
    throw error
  }
}

/** Problem detail is the training session: content from the API, loop state on the client. */
export default async function ProblemPage({ params }: PageProps<"/problems/[slug]">) {
  const { slug } = await params
  const data = await loadTraining(slug)
  if (!data) notFound()

  return (
    <>
      <div className="mx-auto w-full max-w-5xl px-6 pt-6">
        <Breadcrumb
          crumbs={[
            { label: "Problems", href: "/problems" },
            { label: data.problem.pattern.name, href: `/patterns/${data.problem.pattern.slug}` },
            { label: data.problem.title },
          ]}
        />
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="font-mono">
            #{data.problem.externalId}
          </Badge>
          <DifficultyBadge difficulty={data.problem.difficulty} />
          <span className="font-mono text-xs text-muted-foreground">
            {data.problem.complexity.time} time · {data.problem.complexity.space} space
          </span>
        </div>
      </div>
      <TrainingSession
        problem={data.problem}
        patterns={data.patterns}
        hints={data.hints}
        steps={data.steps}
        siblings={data.siblings}
      />
    </>
  )
}

export async function generateMetadata({ params }: PageProps<"/problems/[slug]">) {
  const { slug } = await params
  try {
    const problem = await api.getProblem(slug)
    return { title: problem.title, description: problem.statement.slice(0, 140) }
  } catch {
    return { title: "Problem" }
  }
}