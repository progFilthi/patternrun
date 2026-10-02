import { act, renderHook, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { useTrainingAttempt } from "@/lib/training/use-training-attempt"
import { resetSessionBootstrap } from "@/lib/api/session"
import type { AttemptCodeState, HintSelection } from "@/types/api"

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

    // Suffix matches beat substring matches, and among equals the longest wins.
    //
    // Both tiers are needed. "/api/v1/attempts/{id}/complete" ends with "/complete" but only
    // contains "/attempts", so a suffix-only or length-only rule picks the catch-all and hands
    // back a start response where a completion result was expected. And every attempt URL contains
    // "/attempts", so a nested endpoint such as "/hint/next" would match the catch-all too.
    const keys = Object.keys(table)
    const key =
      keys.filter((fragment) => url.endsWith(fragment)).sort((a, b) => b.length - a.length)[0] ??
      keys.filter((fragment) => url.includes(fragment)).sort((a, b) => b.length - a.length)[0]
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
                             "breakdownCorrect", "codeAccepted", "codeOutcome",
                             "solvedIndependently", "passed", "accepted", "outcome"]) {
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

/**
 * Phase 4: the coding stage.
 *
 * The same rule as every other action, and the same shape of assertion: inspect the actual request
 * body and name the fields that must never appear. Running code is the case where this matters
 * most, because "my code passed" is exactly the claim a determined client would like to make and
 * exactly the one the server must refuse to accept from a string.
 */
describe("the coding stage", () => {
  const WORKING = "def two_sum(nums, target):\n    return [0, 1]\n"

  const RUN_RESULT = {
    problemSlug: "two-sum",
    kind: "RUN" as const,
    outcome: "ACCEPTED" as const,
    casesTotal: 2,
    casesPassed: 2,
    durationMs: 40,
    cases: [
      { ordinal: 1, label: "Example 1", input: "nums = [2,7,11,15], target = 9",
        expected: "[0,1]", actual: "[0, 1]", passed: true, hidden: false },
      { ordinal: 2, label: "Negative values", input: "nums = [-3, 4, 3, 90], target = 0",
        expected: "[0,2]", actual: "[0, 2]", passed: true, hidden: false },
    ],
  }

  async function started(replies: Record<string, unknown> = {}) {
    const calls = stubFetch(replies)
    const view = renderHook(() => useTrainingAttempt("two-sum"))
    await act(async () => {
      await view.result.current.start()
    })
    return { calls, ...view }
  }

  it("runs code against the visible examples and sends nothing but the source", async () => {
    const { calls, result } = await started({ "/code/run": RUN_RESULT })

    await act(async () => {
      await result.current.runCode("PYTHON", WORKING)
    })

    const call = calls.find((entry) => entry.url.includes("/code/run"))!
    expect(call.url).toContain("/api/v1/attempts/attempt-1/code/run")
    expect(call.init?.method).toBe("POST")
    expect(bodyOf(call)).toEqual({ language: "PYTHON", code: WORKING })
  })

  it("submits to a different endpoint than a run", async () => {
    // Run and Submit must not be the same request with a flag, or a caller could reach the
    // evaluation set by accident and a passing Run would start meaning "solved".
    const { calls, result } = await started({ "/code/submit": RUN_RESULT })

    await act(async () => {
      await result.current.submitCode("PYTHON", WORKING)
    })

    const call = calls.find((entry) => entry.url.includes("/code/submit"))!
    expect(call.url).toContain("/api/v1/attempts/attempt-1/code/submit")
    expect(bodyOf(call)).toEqual({ language: "PYTHON", code: WORKING })
  })

  it("never claims that the code passed, in any of the three code requests", async () => {
    const forbidden = [
      "passed", "accepted", "outcome", "correct", "isCorrect", "result", "verdict",
      "casesPassed", "casesTotal", "xp", "awards", "combo", "mastery", "grade",
      "codeAccepted", "codeOutcome", "solvedIndependently", "hidden", "expected",
      "expectedOutput", "input", "testCase", "testCases", "entrypoint", "timeout",
    ]

    const { calls, result } = await started({
      "/code/run": RUN_RESULT,
      "/code/submit": RUN_RESULT,
      "/code/save": { accepted: false, hintsUsed: 0 },
    })

    await act(async () => {
      await result.current.runCode("PYTHON", WORKING)
      await result.current.submitCode("PYTHON", WORKING)
      result.current.persistCode("PYTHON", WORKING)
    })

    const bodies = calls
      .filter((entry) => entry.url.includes("/code/"))
      .map(bodyOf)

    expect(bodies.length).toBeGreaterThanOrEqual(3)
    for (const body of bodies) {
      for (const field of forbidden) {
        expect(body).not.toHaveProperty(field)
      }
      // And nothing beyond the two legitimate keys, whatever the field names happen to be.
      expect(Object.keys(body).sort()).toEqual(["code", "language"])
    }
  })

  it("does not let the caller pick which hint ladder it gets", async () => {
    const { calls, result } = await started({
      "/hint/next": { level: 1, content: "Think about what to remember.",
        stage: "CODING", trigger: "ANY" },
    })

    const captured: { hint: HintSelection | null } = { hint: null }
    await act(async () => {
      captured.hint = await result.current.askForHint("CODING")
    })

    // The stage travels. The trigger does not, because a client that could name its own trigger
    // could ask for the debugging ladder after a passing submission and walk to the answer.
    expect(calls.find((entry) => entry.url.includes("/hint/next"))!.url).toContain("stage=CODING")
    expect(calls.find((entry) => entry.url.includes("/hint/next"))!.url).not.toContain("trigger")
    expect(captured.hint?.content).toContain("remember")
  })

  it("records a revealed rung, because needing help is something the server has to know", async () => {
    const { calls, result } = await started({ "/hint": ATTEMPT })

    await act(async () => {
      await result.current.revealHint(2)
    })

    const call = calls.find((entry) => entry.url.includes("/hint"))!
    expect(bodyOf(call)).toEqual({ level: 2 })
    expect(await result.current.state.hintsUsed).toBe(2)
  })

  it("takes the server's run verdict verbatim and never interprets it", async () => {
    const { result } = await started({ "/code/run": RUN_RESULT })

    await act(async () => {
      await result.current.runCode("PYTHON", WORKING)
    })

    // A passing Run is stored as ACCEPTED because that is what the server said. What it does not
    // become is a solved problem: nothing here sets an accepted flag, because the client does not
    // own that concept. Only the server's submit verdict can mean solved.
    expect(result.current.state.code?.outcome).toBe("ACCEPTED")
    expect(result.current.state.code?.kind).toBe("RUN")
    expect(result.current.state.phase).toBe("active")
  })

  it("reports a runner failure as a failure of ours, not as a wrong answer", async () => {
    const { result } = await started({
      "/code/run": {
        problemSlug: "two-sum",
        kind: "RUN",
        outcome: "INTERNAL_ERROR",
        casesTotal: 0,
        casesPassed: 0,
        cases: [],
        error: { type: "RunnerUnavailable", message: "That is a problem on our side." },
      },
    })

    const captured: { outcome?: string } = {}
    await act(async () => {
      captured.outcome = (await result.current.runCode("PYTHON", WORKING)).outcome
    })

    expect(captured.outcome).toBe("INTERNAL_ERROR")
  })

  it("restores saved source on mount", async () => {
    const { result } = await started({
      "/code": { language: "PYTHON", code: WORKING, accepted: false, hintsUsed: 0 },
    })

    const captured: { state: AttemptCodeState | null } = { state: null }
    await act(async () => {
      captured.state = await result.current.loadCode()
    })

    expect(captured.state?.code).toBe(WORKING)
  })

  it("reveals the reference through a POST, because taking the answer is an event", async () => {
    const { calls, result } = await started({
      "/solution": { problemSlug: "two-sum", language: "PYTHON", code: WORKING, hintsUsed: 0 },
    })

    await act(async () => {
      await result.current.revealReference()
    })

    const call = calls.find((entry) => entry.url.includes("/solution"))!
    expect(call.init?.method).toBe("POST")
    // Nothing is sent about the reveal itself: it is an act, not a claim.
    expect(call.init?.body ?? "{}").toBe("{}")
  })

  it("clears the code phase back to idle once a request settles", async () => {
    const { result } = await started({ "/code/run": RUN_RESULT })

    await act(async () => {
      await result.current.runCode("PYTHON", WORKING)
    })

    expect(result.current.state.codePhase).toBe("idle")
  })

  it("leaves the code phase idle when the request fails, so the panel can retry", async () => {
    const { result } = await started({})
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("{}", { status: 500 })),
    )

    await act(async () => {
      await expect(result.current.runCode("PYTHON", WORKING)).rejects.toThrow()
    })

    expect(result.current.state.codePhase).toBe("idle")
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