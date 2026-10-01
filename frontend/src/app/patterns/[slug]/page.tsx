import Link from "next/link"
import { notFound } from "next/navigation"

import { api, ApiRequestError } from "@/lib/api/client"
import type { PatternDetail, ProblemSummary } from "@/types/api"
import { ProblemList } from "@/components/problems/problem-list"
import { PageHeader, Section } from "@/components/layout/page-header"

export const dynamic = "force-dynamic"

async function loadPattern(slug: string) {
  try {
    const [pattern, problems]: [PatternDetail, ProblemSummary[]] = await Promise.all([
      api.getPattern(slug),
      api.listPatternProblems(slug),
    ])
    return { pattern, problems }
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) return null
    throw error
  }
}

export default async function PatternDetailPage({ params }: PageProps<"/patterns/[slug]">) {
  const { slug } = await params
  const data = await loadPattern(slug)
  if (!data) notFound()
  const { pattern, problems } = data

  return (
    <div className="mx-auto w-full max-w-5xl px-6 pb-24 pt-14">
      <PageHeader
        eyebrow={`pattern ${String(pattern.difficultyOrder).padStart(2, "0")}`}
        title={pattern.name}
        lede={pattern.summary}
      />

      <section aria-label="Signal" className="mt-14 grid gap-10 md:grid-cols-2">
        <div className="flex flex-col gap-3">
          <h2 className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
            Signal
          </h2>
          <p className="text-[15px] leading-relaxed">{pattern.signal}</p>
          <h3 className="mt-4 text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
            Mental model
          </h3>
          <p className="text-[15px] leading-relaxed">{pattern.mentalModel}</p>
        </div>

        <div className="flex flex-col gap-3">
          <h2 className="text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
            Recognition rules
          </h2>
          <ul className="flex flex-col gap-2">
            {pattern.recognitionRules.map((rule) => (
              <li key={rule} className="flex gap-3 text-sm leading-relaxed">
                <span aria-hidden className="text-muted-foreground">
                  ·
                </span>
                <span>{rule}</span>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <Section className="mt-14" title="Template">
        <pre className="mt-5 overflow-x-auto rounded-lg border bg-code p-5 font-mono text-[13px] leading-relaxed">
          <code>{pattern.template.join("\n")}</code>
        </pre>
      </Section>

      <Section className="mt-14" title="Invariant">
        <p className="mt-5 max-w-3xl border-l-2 border-foreground/20 pl-4 text-[15px] leading-relaxed">
          {pattern.invariant}
        </p>
      </Section>

      <Section
        className="mt-16"
        title="Problems"
        meta={`${problems.length} in this pattern`}
        action={
          <Link
            href="/problems"
            className="text-xs text-muted-foreground underline underline-offset-4 transition-colors hover:text-foreground"
          >
            All problems
          </Link>
        }
      >
        <ProblemList problems={problems} />
      </Section>
    </div>
  )
}

export async function generateMetadata({ params }: PageProps<"/patterns/[slug]">) {
  const { slug } = await params
  try {
    const pattern = await api.getPattern(slug)
    return { title: pattern.name, description: pattern.signal }
  } catch {
    return { title: "Pattern" }
  }
}