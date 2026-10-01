"use client"

import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/layout/page-header"
import Link from "next/link"

/** Friendly, non-technical states (README sections 109 and 110). */
export default function Error({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const apiUnreachable = error.message.includes("not reachable")

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-8 px-6 pt-20">
      <PageHeader
        eyebrow={apiUnreachable ? "the api is waking up" : "something went wrong"}
        title={
          apiUnreachable
            ? "This can take a moment on the free hosting tier."
            : "Could not load this problem."
        }
        lede={
          apiUnreachable
            ? "The API sleeps when it is idle. Retry in a few seconds and the content will load."
            : "The content did not come back. Try again, and if it keeps failing check that the API is running."
        }
      />
      <div className="flex gap-3">
        <Button onClick={reset}>Try again</Button>
        <Button variant="outline" asChild>
          <Link href="/">Back to dashboard</Link>
        </Button>
      </div>
    </div>
  )
}