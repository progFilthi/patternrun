import { render, screen } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"

import { CompletionSummary } from "@/components/training/completion-summary"
import type { SessionState } from "@/lib/training/use-training-attempt"
import type { CompletionResult, ProblemDetail, ProblemSummary } from "@/types/api"

/**
 * What the completion screen shows.
 *
 * Every assertion here is that a value from the backend appears as given. Nothing in this file
 * calculates anything: the component renders {@link CompletionResult} and these tests check it
 * does so faithfully, including the awkward cases, a repeat worth nothing and a result that
 * never arrived.
 */

const problem = {
  slug: "two-sum",
  title: "Two Sum",
  pattern: { slug: "hashing", name: "Hashing" },
  invariant: "seen contains exactly the numbers already visited.",
  interviewExplanation: "I use a HashMap from value to index.",
} as unknown as ProblemDetail

function stateWith(overrides: Partial<SessionState>): SessionState {
  return { phase: "complete", hintsUsed: 0, predictions: [], breakdownPending: false, ...overrides }
}

const EARNED: CompletionResult = {
  attemptId: "a1",
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
  combo: 3,
  awards: [
    { reason: "PATTERN_IDENTIFIED", label: "Pattern identified", xp: 10, alreadyEarned: false },
    { reason: "PROBLEM_COMPLETED", label: "Problem solved", xp: 50, alreadyEarned: false },
    { reason: "NO_HINTS", label: "No hints needed", xp: 20, alreadyEarned: false },
  ],
  patternSlug: "hashing",
  patternMastery: 80,
  personalBest: false,
  mistakesResolved: [],
}

function renderSummary(state: SessionState, nextProblem?: ProblemSummary) {
  return render(
    <CompletionSummary
      problem={problem}
      state={state}
      onRetry={() => {}}
      nextProblem={nextProblem}
    />,
  )
}

describe("a successful completion", () => {
  it("shows the XP total the backend sent", () => {
    renderSummary(stateWith({ result: EARNED }))

    expect(screen.getByText("+115 XP")).toBeDefined()
  })

  it("shows each award with the backend's own wording", () => {
    renderSummary(stateWith({ result: EARNED }))

    expect(screen.getByText("Pattern identified")).toBeDefined()
    expect(screen.getByText("Problem solved")).toBeDefined()
    expect(screen.getByText("No hints needed")).toBeDefined()
    expect(screen.getAllByText("+10").length).toBeGreaterThan(0)
    expect(screen.getByText("+50")).toBeDefined()
  })

  it("shows the combo the backend calculated", () => {
    renderSummary(stateWith({ result: EARNED }))

    // Uppercased by CSS, which is why the assertion reads the rendered text rather than the node.
    expect(screen.getByText(/combo ×3/i)).toBeDefined()
  })

  it("hides the combo when there is no run", () => {
    renderSummary(stateWith({ result: { ...EARNED, combo: 1 } }))

    expect(screen.queryByText(/combo/i)).toBeNull()
  })

  it("shows the mastery the backend calculated", () => {
    renderSummary(stateWith({ result: EARNED }))

    expect(screen.getByText("80%")).toBeDefined()
    expect(screen.getByRole("meter", { name: "Pattern mastery" }).getAttribute("aria-valuenow")).toBe("80")
  })

  it("omits the mastery bar when the backend sent none", () => {
    // Mastery is absent for a pattern that has never been measured, which is not the same as
    // zero. Destructured rather than deleted by key so the intent stays readable.
    const { patternMastery, ...withoutMastery } = EARNED
    expect(patternMastery).toBe(80)
    renderSummary(stateWith({ result: withoutMastery as CompletionResult }))

    expect(screen.queryByRole("meter")).toBeNull()
    expect(screen.queryByText(/%/)).toBeNull()
  })

  it("shows the grade, and flags a provisional one", () => {
    const { unmount } = renderSummary(stateWith({ result: EARNED }))
    expect(screen.getByText(/strong/i)).toBeDefined()
    unmount()

    renderSummary(stateWith({ result: { ...EARNED, provisional: true } }))
    expect(screen.getByText(/provisional/i)).toBeDefined()
  })

  it("never exposes the internal award identifiers", () => {
    const { container } = renderSummary(stateWith({ result: EARNED }))

    expect(container.textContent).not.toContain("PROBLEM_COMPLETED")
    expect(container.textContent).not.toContain("PATTERN_IDENTIFIED")
    expect(container.textContent).not.toContain("COMBO_BONUS")
  })
})

describe("a repeat that earned nothing", () => {
  const repeat: CompletionResult = {
    ...EARNED,
    totalXpAwarded: 0,
    combo: 3,
    awards: [],
  }

  it("says no new XP rather than showing zero as a score", () => {
    renderSummary(stateWith({ result: repeat }))

    expect(screen.getByText("No new XP")).toBeDefined()
    expect(screen.queryByText("+0 XP")).toBeNull()
  })

  it("explains why, so an empty award list does not look broken", () => {
    renderSummary(stateWith({ result: repeat }))

    expect(screen.getByText(/already been paid for this one/i)).toBeDefined()
  })

  it("shows no award rows", () => {
    renderSummary(stateWith({ result: repeat }))

    expect(screen.queryByText("Problem solved")).toBeNull()
  })
})

describe("states before a result exists", () => {
  it("shows a waiting message while completing", () => {
    renderSummary(stateWith({ phase: "completing" }))

    expect(screen.getByRole("status")).toBeDefined()
    expect(screen.queryByText(/XP/)).toBeNull()
  })

  it("shows the reason and a retry on failure, and no invented total", () => {
    const onRetry = vi.fn()
    render(
      <CompletionSummary
        problem={problem}
        state={stateWith({
          phase: "failed",
          error: "Cannot reach PatternRun right now. Your session is safe.",
          retryable: true,
        })}
        onRetry={onRetry}
      />,
    )

    expect(screen.getByRole("alert")).toBeDefined()
    expect(screen.getByText(/Cannot reach PatternRun/)).toBeDefined()
    // The point of the whole state: never a fabricated number.
    expect(screen.queryByText(/XP/)).toBeNull()
    expect(screen.queryByText(/No new XP/)).toBeNull()
  })

  it("offers a retry the learner can take", () => {
    const onRetry = vi.fn()
    render(
      <CompletionSummary
        problem={problem}
        state={stateWith({ phase: "failed", error: "Something went wrong.", retryable: true })}
        onRetry={onRetry}
      />,
    )

    screen.getByRole("button", { name: /try again/i }).click()
    expect(onRetry).toHaveBeenCalledTimes(1)
  })

  it("does not offer a retry that cannot help", () => {
    render(
      <CompletionSummary
        problem={problem}
        state={stateWith({ phase: "failed", error: "This session never started.", retryable: false })}
        onRetry={() => {}}
      />,
    )

    expect(screen.queryByRole("button", { name: /try again/i })).toBeNull()
  })

  it("says the result has not arrived rather than showing an empty summary", () => {
    renderSummary(stateWith({ phase: "active" }))

    expect(screen.getByText(/has not come back yet/i)).toBeDefined()
    expect(screen.queryByText("No new XP")).toBeNull()
  })
})

describe("where to go next", () => {
  it("offers the next problem when there is one", () => {
    renderSummary(
      stateWith({ result: EARNED }),
      { slug: "3sum", title: "3Sum" } as ProblemSummary,
    )

    expect(screen.getByRole("link", { name: /Next problem: 3Sum/ })).toBeDefined()
  })

  it("offers the library when there is not", () => {
    renderSummary(stateWith({ result: EARNED }))

    expect(screen.getByRole("link", { name: "Back to problems" })).toBeDefined()
  })
})