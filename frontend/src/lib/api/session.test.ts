import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { reviewMistake, SessionApiError } from "@/lib/api/session"
import type { ReviewOutcome } from "@/types/api"

/**
 * The wire contract for reviewing a mistake.
 *
 * This is the narrowest request in the whole app and the easiest one to quietly widen. A review
 * pays XP, moves the due date and counts as a recall, so anything added to this body is a lever the
 * learner could pull. These tests hold the request to exactly one field: what they believe about
 * their own understanding. The interval, the award and the next due date are the server's to
 * decide, and the 409 is how it says "not yet" without the client having to guess.
 */

const OUTCOME: ReviewOutcome = {
  mistakeId: "m1",
  problemSlug: "two-sum",
  category: "PATTERN",
  correct: true,
  reviewCount: 1,
  intervalDays: 2,
  nextDueAt: "2026-01-03T00:00:00Z",
  xpAwarded: 20,
  alreadyEarned: false,
}

function respondWith(body: unknown, status = 200): void {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      ok: status < 400,
      status,
      json: async () => body,
    }),
  )
}

function lastRequest(): [string, RequestInit] {
  const call = vi.mocked(fetch).mock.calls.at(-1)
  if (!call) throw new Error("no request was made")
  return call as [string, RequestInit]
}

beforeEach(() => {
  vi.clearAllMocks()
  respondWith(OUTCOME)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe("what the browser sends", () => {
  it("posts to the mistake being reviewed, under this origin", async () => {
    await reviewMistake("m1", true)
    expect(lastRequest()[0]).toBe("/api/v1/progress/review/m1")
  })

  it("sends the answer and nothing else", async () => {
    await reviewMistake("m1", true)
    expect(JSON.parse(String(lastRequest()[1].body))).toEqual({ correct: true })
  })

  it("cannot be made to claim an interval, an award, or a due date", async () => {
    // The type signature already forbids this. This asserts the guard is still there rather than
    // trusting a review of the signature to catch it.
    await reviewMistake("m1", true)
    const body = JSON.parse(String(lastRequest()[1].body)) as Record<string, unknown>
    expect(Object.keys(body)).toEqual(["correct"])
  })

  it("says no honestly too, rather than requiring a positive answer to be sent", async () => {
    await reviewMistake("m1", false)
    expect(JSON.parse(String(lastRequest()[1].body))).toEqual({ correct: false })
  })

  it("attaches the session cookie, because this request is only meaningful with one", async () => {
    await reviewMistake("m1", true)
    expect(lastRequest()[1].credentials).toBe("include")
  })
})

describe("what the browser takes back", () => {
  it("returns the server's outcome rather than one it assembled", async () => {
    await expect(reviewMistake("m1", true)).resolves.toEqual(OUTCOME)
  })

  it("takes the interval from the server even when it disagrees with the input", async () => {
    respondWith({ ...OUTCOME, intervalDays: 14, xpAwarded: 0, alreadyEarned: true })
    await expect(reviewMistake("m1", true)).resolves.toMatchObject({
      intervalDays: 14,
      xpAwarded: 0,
      alreadyEarned: true,
    })
  })
})

describe("when the server declines", () => {
  it("reports a mistake that is not due yet as a conflict the UI can explain", async () => {
    respondWith({ message: "That mistake is not due yet." }, 409)

    const failure = await reviewMistake("m1", true).catch((error: unknown) => error)

    expect(failure).toBeInstanceOf(SessionApiError)
    expect((failure as SessionApiError).kind).toBe("conflict")
    expect((failure as SessionApiError).message).toContain("not due yet")
  })

  it("does not offer a conflict for a retry, because retrying sooner cannot help", async () => {
    respondWith({ message: "not due" }, 409)
    const failure = await reviewMistake("m1", true).catch((error: unknown) => error)
    expect((failure as SessionApiError).isRetryable).toBe(false)
  })

  it("marks a missing session as such, so the UI can send the learner to sign in", async () => {
    respondWith({ message: "no session" }, 401)
    const failure = await reviewMistake("m1", true).catch((error: unknown) => error)
    expect((failure as SessionApiError).kind).toBe("session")
  })

  it("treats an unreachable API as worth retrying", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
    )
    const failure = await reviewMistake("m1", true).catch((error: unknown) => error)
    expect((failure as SessionApiError).kind).toBe("offline")
    expect((failure as SessionApiError).isRetryable).toBe(true)
  })

  it("keeps the server's reason even when it is not one this client knows about", async () => {
    respondWith({ error: "mistake belongs to another learner" }, 404)
    const failure = await reviewMistake("m1", true).catch((error: unknown) => error)
    expect((failure as SessionApiError).message).toBe("mistake belongs to another learner")
  })
})