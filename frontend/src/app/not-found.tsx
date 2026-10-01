import Link from "next/link"

import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/layout/page-header"

export default function NotFound() {
  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-8 px-6 pt-20">
      <PageHeader
        eyebrow="not found"
        title="Nothing to train here."
        lede="That pattern or problem is not in the library yet."
      />
      <div className="flex gap-3">
        <Button asChild>
          <Link href="/patterns">Browse patterns</Link>
        </Button>
        <Button variant="outline" asChild>
          <Link href="/problems">Browse problems</Link>
        </Button>
      </div>
    </div>
  )
}
