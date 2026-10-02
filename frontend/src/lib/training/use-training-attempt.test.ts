import { act, renderHook, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { useTrainingAttempt } from "@/lib/training/use-training-attempt"
import { resetSessionBootstrap } from "@/lib/api/session"

/**
 * The wire contract, which is where the security rules live.
 *
 * The most important assertions in this file are the ones that inspect a request body. They
 * exist to prove the client cannot influence its own reward: every payload must contain only
 * what the learner chose, and nothing that the server is supposed to decide. A future change
 * that helpfully started sending `patternCorrect` or a predicted total would fail here.
 */

const ATTEMPT = {
  id: "attempt-1",
  problemSlug: "two-sum",
  mode: "STANDARD" as const,
  status: "IN_PROGRESS" as const,
  hintsUsed: 0,
  patternCorrect: false,
  predictionsAnswered: 0,
  predictionsCorrect: 0,
  complexityCorrect: false,
  breakdownCorrect: false,
}

const RESULT = {
  attemptId: "attempt-1",
  problemSlug: "two-sum",
  grade: "STRONG",
  provisional: false,
  patternCorrect: true,
  complexityCorrect: true,
  breakdownCorrect: false,
  predictionsAnswered: 1,
  predictionsCorrect: 1,
  hintsUsed: 0,
  durationMs: 120000,
  totalXpAwarded: 115,
  combo: 2,
  awards: [{ reason: "PROBLEM_COMPLETED", label: "Problem solved", xp: 50, alreadyEarned: false }],
  patternSlug: "hashing",
  patternMastery: 80,
  personalBest: false,
  mistakesResolved: [],
}

interface Call {
  url: string
  init?: RequestInit
}

/**
 * Records every request and replies from a queue keyed by URL fragment.
 *
 * `/attempts` is answered by default. Every case needs the session to exist before it can do
 * anything else, so making each one restate that would be noise and an easy place to forget.
 */
function stubFetch(replies: Record<string, unknown> = {}) {
  const calls: Call[] = []
  const table: Record<string, unknown> = { "/attempts": ATTEMPT, ...replies }

  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    calls.push({ url, init })

    // Matched on the tail first. A substring match would let "/api/v1/attempts/{id}/complete"
    // match the "/attempts" reply and hand back a start response where a result was expected,
    // which fails as a confusing assertion rather than as an obvious stub bug.
    const key =
      Object.keys(table).find((fragment) => url.endsWith(fragment)) ??
      Object.keys(table).find((fragment) => url.includes(fragment))
    if (key === undefined) {
      return new Response(JSON.stringify({ message: `unexpected call: ${url}` }), { status: 404 })
    }
    return new Response(JSON.stringify(table[key]), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    })
  })

  vi.stubGlobal("fetch", fetchMock)
  return calls
}

function bodyOf(call: Call): Record<string, unknown> {
  return JSON.parse(String(call.init?.body ?? "{}"))
}

beforeEach(() => {
  resetSessionBootstrap()
})

describe("starting a session", () => {
  it("opens an attempt and remembers its id", async () => {
    stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })

    await waitFor(() => expect(result.current.state.phase).toBe("active"))
    expect(result.current.state.attemptId).toBe("attempt-1")
  })

  it("sends the problem slug and nothing else", async () => {
    const calls = stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })

    expect(bodyOf(calls[0])).toEqual({ problemSlug: "two-sum", mode: "STANDARD" })
  })

  it("reports a failure rather than pretending to have started", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify({ message: "boom" }), { status: 500 })),
    )
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })

    await waitFor(() => expect(result.current.state.phase).toBe("failed"))
    expect(result.current.state.retryable).toBe(true)
  })
})

describe("submitting learner actions", () => {
  it("sends the chosen pattern slug and nothing else", async () => {
    const calls = stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })
    await act(async () => {
      await result.current.choosePattern("hashing")
    })

    const call = calls.find((entry) => entry.url.endsWith("/pattern"))
    expect(bodyOf(call!)).toEqual({ patternSlug: "hashing" })
    // No self-report of correctness, and nothing resembling a score.
    expect(bodyOf(call!)).not.toHaveProperty("patternCorrect")
  })

  it("sends the chosen option and never a claim about it", async () => {
    const calls = stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })
    await act(async () => {
      await result.current.predict(3, 1)
    })

    const call = calls.find((entry) => entry.url.includes("/predict"))
    // The step and the option, and nothing else. The answer already reached this browser, so
    // sending a verdict would be the client grading its own homework.
    expect(bodyOf(call!)).toEqual({ stepOrder: 3, chosenIndex: 1 })
    expect(bodyOf(call!)).not.toHaveProperty("correct")
  })

  it("sends a hint level as a number", async () => {
    const calls = stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })
    await act(async () => {
      await result.current.revealHint(3)
    })

    const call = calls.find((entry) => entry.url.includes("/hint"))
    expect(bodyOf(call!)).toEqual({ level: 3 })
  })

  it("shows an action is in flight and then settles back", async () => {
    stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })
    await act(async () => {
      await result.current.revealHint(1)
    })

    await waitFor(() => expect(result.current.state.phase).toBe("active"))
    expect(result.current.state.hintsUsed).toBe(1)
  })
})

describe("completing", () => {
  it("sends observations only, with no XP, awards or correctness", async () => {
    const calls = stubFetch({ "/complete": RESULT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })
    await act(async () => {
      await result.current.predict(3, 1)
    })
    await act(async () => {
      await result.current.complete({
        complexityTime: "O(n)",
        complexitySpace: "O(n)",
        durationMs: 120000,
      })
    })

    const call = calls.find((entry) => entry.url.includes("/complete"))
    const body = bodyOf(call!)

    expect(body).toEqual({
      complexityTime: "O(n)",
      complexitySpace: "O(n)",
      durationMs: 120000,
      predictions: [{ stepOrder: 3, chosenIndex: 1 }],
    })

    // The explicit version of the rule, so a future field cannot slip in unnoticed.
    // breakdownCorrect is in this list deliberately: the field was removed from the contract,
    // and the server now derives it from the answers recorded at the breakdown step.
    for (const forbidden of ["xp", "totalXp", "awards", "combo", "mastery", "grade",
                             "patternCorrect", "complexityCorrect", "predictionsCorrect",
                             "breakdownCorrect"]) {
      expect(body).not.toHaveProperty(forbidden)
    }
  })

  it("takes the backend's result verbatim", async () => {
    stubFetch({ "/complete": RESULT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })
    await act(async () => {
      await result.current.complete({
        complexityTime: "O(n)",
        complexitySpace: "O(n)",
        durationMs: 120000,
      })
    })

    await waitFor(() => expect(result.current.state.phase).toBe("complete"))
    expect(result.current.state.result?.totalXpAwarded).toBe(115)
    expect(result.current.state.result?.combo).toBe(2)
    expect(result.current.state.result?.awards).toEqual(RESULT.awards)
    expect(result.current.state.result?.patternMastery).toBe(80)
  })

  it("surfaces a zero-XP repeat as the server reported it", async () => {
    stubFetch({
      "/complete": { ...RESULT, totalXpAwarded: 0, combo: 2, awards: [] },
    })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })
    await act(async () => {
      await result.current.complete({
        complexityTime: "O(n)",
        complexitySpace: "O(n)",
        durationMs: 120000,
      })
    })

    await waitFor(() => expect(result.current.state.phase).toBe("complete"))
    expect(result.current.state.result?.totalXpAwarded).toBe(0)
    expect(result.current.state.result?.awards).toEqual([])
  })

  it("does not send a second completion while one is in flight", async () => {
    const calls = stubFetch({ "/complete": RESULT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })

    const input = {
      complexityTime: "O(n)",
      complexitySpace: "O(n)",
      breakdownCorrect: false,
      durationMs: 120000,
    }

    // Two completions in the same tick, which is what a double click or a retry storm looks like.
    await act(async () => {
      const first = result.current.complete(input)
      const second = result.current.complete(input)
      await Promise.all([first, second])
    })

    await waitFor(() => expect(result.current.state.phase).toBe("complete"))
    expect(calls.filter((entry) => entry.url.includes("/complete"))).toHaveLength(1)
  })

  it("refuses to complete a session that never started", async () => {
    stubFetch({})
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.complete({
        complexityTime: "O(n)",
        complexitySpace: "O(n)",
        durationMs: 120000,
      })
    })

    expect(result.current.state.phase).toBe("failed")
    expect(result.current.state.retryable).toBe(false)
  })
})

describe("failure states", () => {
  it("reports a network failure and offers a retry", async () => {
    stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })

    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch")
      }),
    )

    await act(async () => {
      await result.current.complete({
        complexityTime: "O(n)",
        complexitySpace: "O(n)",
        durationMs: 120000,
      })
    })

    await waitFor(() => expect(result.current.state.phase).toBe("failed"))
    expect(result.current.state.retryable).toBe(true)
    // The important part: no invented result while the backend is unreachable.
    expect(result.current.state.result).toBeUndefined()
  })

  it("reports a lost session with the server's own message", async () => {
    stubFetch({ "/attempts": ATTEMPT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })

    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ message: "Sign in to continue." }), { status: 401 }),
      ),
    )

    await act(async () => {
      await result.current.complete({
        complexityTime: "O(n)",
        complexitySpace: "O(n)",
        durationMs: 120000,
      })
    })

    await waitFor(() => expect(result.current.state.phase).toBe("failed"))
    expect(result.current.state.error).toBe("Sign in to continue.")
    // A lost session is recoverable by the next call, so it must not read as a dead end.
    expect(result.current.state.retryable).toBe(true)
    expect(result.current.state.result).toBeUndefined()
  })

  it("re-sends the original payload on retry, not a rebuilt one", async () => {
    stubFetch({ "/complete": RESULT })
    const { result } = renderHook(() => useTrainingAttempt("two-sum"))

    await act(async () => {
      await result.current.start()
    })

    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch")
      }),
    )
    const input = {
      complexityTime: "O(log n)",
      complexitySpace: "O(n)",
      breakdownCorrect: false,
      durationMs: 42000,
    }
    await act(async () => {
      await result.current.complete(input)
    })
    await waitFor(() => expect(result.current.state.phase).toBe("failed"))

    const calls = stubFetch({ "/complete": RESULT })
    await act(async () => {
      await result.current.retry()
    })

    await waitFor(() => expect(result.current.state.phase).toBe("complete"))
    expect(bodyOf(calls[0])).toMatchObject({ complexityTime: "O(log n)", durationMs: 42000 })
  })
})