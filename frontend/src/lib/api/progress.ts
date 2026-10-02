import { cookies } from "next/headers"

import type { ProblemProgress, ProgressSummary, ReviewItem } from "@/types/api"

/**
 * Server-side reads of the learner's progress.
 *
 * The caller's cookie is forwarded, so a server component sees exactly the progress the browser
 * would. Without this a server-rendered list would show nothing completed for every signed-in
 * learner, which is the kind of bug that only shows up in production.
 *
 * Reads go straight to the API rather than through the frontend's own rewrite, which skips a
 * pointless hop for content and keeps this on the same footing as the other server reads.
 */

const serverUrl =
  process.env.API_INTERNAL_URL ?? process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"

/**
 * A failure here is never fatal.
 *
 * Content is what this app is for. A learner whose progress cannot be read should still be able
 * to read problems, so an unavailable summary renders as an empty one rather than as an error
 * page. That is honest: the list shows nothing completed, which is visibly different from a
 * fabricated tick.
 */
async function read<T>(path: string): Promise<T | null> {
  try {
    const cookieHeader = (await cookies()).toString()
    const response = await fetch(`${serverUrl.replace(/\/$/, "")}/api/v1${path}`, {
      headers: {
        Accept: "application/json",
        ...(cookieHeader ? { cookie: cookieHeader } : {}),
      },
      cache: "no-store",
    })
    if (!response.ok) return null
    return (await response.json()) as T
  } catch {
    return null
  }
}

/**
 * Everything a progress screen needs, in one call.
 *
 * The backend serves this as one endpoint rather than six so the screen paints once instead of
 * assembling itself from partial data.
 */
export function getProgressSummary(): Promise<ProgressSummary | null> {
  return read<ProgressSummary>("/progress")
}

/**
 * The mistakes that are actually due. Read-only: reviewing is a separate, explicit act.
 *
 * An empty list when the API cannot be reached, because a queue that fails to load should read as
 * "nothing due" rather than as an error on a page that is otherwise fine.
 */
export function getReviewQueue(): Promise<ReviewItem[]> {
  return read<ReviewItem[]>("/progress/review").then((items) => items ?? [])
}

/** Per-problem state for the library list. */
export function getProblemProgress(): Promise<ProblemProgress[]> {
  return read<ProblemProgress[]>("/progress/problems").then((result) => result ?? [])
}

/**
 * The slugs this learner has completed, for marking a list.
 *
 * An empty set when unknown, so a list renders without progress rather than failing.
 */
export async function getCompletedSlugs(): Promise<Set<string>> {
  const states = await getProblemProgress()
  return new Set(states.filter((state) => state.completed).map((state) => state.problemSlug))
}