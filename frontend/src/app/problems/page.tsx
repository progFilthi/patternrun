import Link from "next/link"

import { api } from "@/lib/api/client"
import { ProblemList } from "@/components/problems/problem-list"
import { PageHeader } from "@/components/layout/page-header"

export const metadata = { title: "Problems" }
export const dynamic = "force-dynamic"

export default async function ProblemsPage({ searchParams }: PageProps<"/problems">) {
  const { pattern: rawPattern } = await searchParams
  // A repeated query parameter arrives as an array; only the first value is meaningful.
  const pattern = (Array.isArray(rawPattern) ? rawPattern[0] : rawPattern) ?? ""
  const [patterns, page] = await Promise.all([
    api.listPatterns(),
    api.listProblems({ pattern: pattern || undefined, size: 100 }),
  ])

  return (
    <div className="mx-auto w-full max-w-5xl px-6 pb-24 pt-14">
      <PageHeader
        eyebrow="problem library"
        title="Pick one problem and go deep."
        lede={`${page.totalElements} problems across ${patterns.length} patterns. Filter by pattern to narrow the list.`}
      />

      <nav aria-label="Filter by pattern" className="mt-10 flex flex-wrap gap-2">
        <FilterLink href="/problems" active={!pattern}>
          All
        </FilterLink>
        {patterns.map((item) => (
          <FilterLink
            key={item.slug}
            href={`/problems?pattern=${item.slug}`}
            active={pattern === item.slug}
          >
            {item.name}
          </FilterLink>
        ))}
      </nav>

      <div className="mt-10">
        <ProblemList problems={page.content} />
      </div>
    </div>
  )
}

function FilterLink({
  href,
  active,
  children,
}: {
  href: string
  active: boolean
  children: React.ReactNode
}) {
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={`rounded-md border px-3 py-1.5 text-sm transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none ${
        active
          ? "border-transparent bg-foreground font-medium text-background"
          : "border-border text-muted-foreground hover:border-foreground/40 hover:bg-muted hover:text-foreground"
      }`}
    >
      {children}
    </Link>
  )
}