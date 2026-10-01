import { api } from "@/lib/api/client"
import { PatternList } from "@/components/patterns/pattern-list"
import { PageHeader } from "@/components/layout/page-header"

export const metadata = { title: "Patterns" }
export const dynamic = "force-dynamic"

export default async function PatternsPage() {
  const patterns = await api.listPatterns()

  return (
    <div className="mx-auto w-full max-w-6xl px-6 pb-24 pt-14">
      <PageHeader
        eyebrow="pattern system"
        title="Ten patterns cover most problems."
        lede="Learn the signal first. The problem number comes second."
      />

      <div className="mt-14">
        <PatternList patterns={patterns} />
      </div>
    </div>
  )
}