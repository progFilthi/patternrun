import { describe, expect, it, vi } from "vitest"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"

import type { ExecutionResult, HintSelection, RevealedSolution } from "@/types/api"

/**
 * The coding stage, as a learner meets it.
 *
 * CodeMirror is stubbed. What is under test here is the decisions this screen makes — which result
 * is shown, what a wrong answer looks like next to a crash, and that a passing Run is never
 * presented as a solve — and none of those live inside the editor. The editor itself is a thin,
 * separately isolated wrapper whose job is to load a grammar.
 */

vi.mock("@/components/editor/code-editor", () => ({
  CodeEditor: ({
    value,
    onChange,
    label,
    readOnly,
  }: {
    value: string
    onChange: (value: string) => void
    label: string
    readOnly?: boolean
  }) => (
    <div>
      <span>{label}</span>
      <textarea
        aria-label={label}
        value={value}
        readOnly={readOnly}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  ),
}))

const { CodePanel } = await import("@/components/training/code-panel")

const RESULT: ExecutionResult = {
  problemSlug: "two-sum",
  kind: "SUBMIT",
  outcome: "ACCEPTED",
  casesTotal: 7,
  casesPassed: 7,
  durationMs: 42,
  cases: [
    { ordinal: 1, label: "Example 1", input: "nums = [2,7,11,15], target = 9",
      expected: "[0,1]", actual: "[0, 1]", passed: true, hidden: false },
    { ordinal: 3, passed: true, hidden: true },
    { ordinal: 4, passed: true, hidden: true },
  ],
}

function panel(overrides: Partial<Parameters<typeof CodePanel>[0]> = {}) {
  return render(
    <CodePanel
      attemptId="attempt-1"
      entrypoint="two_sum"
      onSave={vi.fn()}
      onRun={vi.fn(async () => RESULT)}
      onSubmit={vi.fn(async () => RESULT)}
      onHint={vi.fn(async () => null)}
      onReveal={vi.fn(async () => ({ problemSlug: "two-sum", language: "PYTHON", code: "def two_sum(): pass", hintsUsed: 0 }))}
      loadState={async () => ({ accepted: false, hintsUsed: 0 })}
      onHintRead={vi.fn()}
      {...overrides}
    />,
  )
}

function click(name: RegExp) {
  fireEvent.click(screen.getByRole("button", { name }))
}

describe("the coding stage", () => {
  it("names the function it will call, so the signature is not a guess", () => {
    panel()
    expect(screen.getByText("two_sum")).toBeDefined()
  })

  it("explains that Run and Submit are different acts", () => {
    const { container } = panel()
    // The learner has to know that pressing Run five times is not the same as finishing.
    // Asserted on the container because the sentence is broken up by <strong> elements, which is
    // what it should be: the emphasis is on the two verbs and nothing else.
    expect(container.textContent).toMatch(/Submit.*full/i)
  })

  it("offers Run and Submit as separate controls", () => {
    panel()
    expect(screen.getByRole("button", { name: /Run/ })).toBeDefined()
    expect(screen.getByRole("button", { name: /Submit/ })).toBeDefined()
  })

  it("does not show a score, a counter or a reward anywhere", async () => {
    const { container } = panel()
    await screen.findByRole("button", { name: /Run/ })

    // The coding experience communicates correctness and progress. It does not pay out.
    expect(container.textContent).not.toMatch(/\\bXP\\b/)
    expect(container.textContent).not.toMatch(/combo/i)
    expect(container.textContent).not.toMatch(/mastery/i)
  })
})

describe("Run", () => {
  const RUN_RESULT: ExecutionResult = { ...RESULT, kind: "RUN", casesTotal: 2, casesPassed: 2 }

  it("shows the visible examples and how many passed", async () => {
    panel({ onRun: vi.fn(async () => RUN_RESULT) })
    click(/Run/)

    await waitFor(() => expect(screen.getByText("Example 1")).toBeDefined())
    // Twice on purpose: once as the headline verdict and once as the sentence that says what to do
    // about it. Both are asserted so neither can be quietly dropped.
    expect(screen.getAllByText(/Visible examples pass/).length).toBeGreaterThanOrEqual(2)
  })

  it("says plainly that a passing Run is not a solve", async () => {
    // The single most important sentence on this screen. Without it a learner assumes the
    // examples were the test, and stops before the thing that actually decides.
    panel({ onRun: vi.fn(async () => RUN_RESULT) })
    click(/Run/)

    await waitFor(() =>
      expect(screen.getByText(/Visible examples pass.*Submit to be checked/i)).toBeDefined(),
    )
  })

  it("reports only a count for hidden cases, never a label or its input", async () => {
    panel()
    click(/Run/)

    await waitFor(() => expect(screen.getByText(/hidden cases passed/)).toBeDefined())
    // A hidden label would hand over the evaluation set.
    expect(screen.queryByText("Example 3")).toBeNull()
  })
})

describe("when the code fails", () => {
  const wrong: ExecutionResult = {
    problemSlug: "two-sum",
    kind: "SUBMIT",
    outcome: "WRONG_ANSWER",
    casesTotal: 7,
    casesPassed: 3,
    cases: [
      { ordinal: 1, label: "Example 1", input: "nums = [2,7,11,15], target = 9",
        expected: "[0,1]", actual: "[1, 0]", passed: false, hidden: false },
      { ordinal: 2, label: "Negative values", input: "nums = [-3, 4, 3, 90], target = 0",
        expected: "[0,2]", actual: "[1, 2]", passed: false, hidden: false },
      { ordinal: 3, passed: true, hidden: true },
    ],
  }

  it("shows expected beside actual, so there is something to reason about", async () => {
    const { container } = panel({ onSubmit: vi.fn(async () => wrong) })
    click(/Submit/)

    await waitFor(() => expect(screen.getByText("Wrong answer")).toBeDefined())
    // The labels are their own elements so they can be quiet, and the values are their own
    // elements so they can be monospaced. Asserted on the rendered text rather than on elements.
    expect(container.textContent).toContain("expected [0,1]")
    expect(container.textContent).toContain("you returned [1, 0]")
  })

  it("does not offer the solution when the answer was merely wrong", async () => {
    panel({ onSubmit: vi.fn(async () => wrong) })
    click(/Submit/)

    await waitFor(() => expect(screen.getByText("Wrong answer")).toBeDefined())
    // A wrong answer is a reasoning problem. Jumping straight to the implementation would skip
    // the part of the loop that is supposed to teach.
    expect(screen.queryByText("Reference solution")).toBeNull()
  })

  it("reports a crash with its line, and does not claim a wrong answer", async () => {
    const crashed: ExecutionResult = {
      problemSlug: "two-sum",
      kind: "SUBMIT",
      outcome: "RUNTIME_ERROR",
      casesTotal: 0,
      casesPassed: 0,
      error: {
        type: "TypeError",
        line: 3,
        message: "unsupported operand type(s) for -: 'int' and 'list'",
      },
      cases: [],
    }
    panel({ onSubmit: vi.fn(async () => crashed) })
    click(/Submit/)

    await waitFor(() => expect(screen.getByText("Runtime error")).toBeDefined())
    expect(screen.getByText("TypeError")).toBeDefined()
    expect(screen.getByText("line 3")).toBeDefined()
    expect(screen.queryByText("Wrong answer")).toBeNull()
  })

  it("tells a syntax error apart from a crash, because the fix is different", async () => {
    panel({
      onSubmit: vi.fn(async () => ({
        problemSlug: "two-sum",
        kind: "SUBMIT" as const,
        outcome: "SYNTAX_ERROR" as const,
        casesTotal: 0,
        casesPassed: 0,
        cases: [],
        error: { type: "SyntaxError", line: 4, message: "expected ':'" },
      })),
    })
    click(/Submit/)

    await waitFor(() => expect(screen.getByText("Syntax error")).toBeDefined())
  })

  it("calls a timeout a complexity problem rather than a wrong answer", async () => {
    panel({
      onSubmit: vi.fn(async () => ({
        problemSlug: "two-sum",
        kind: "SUBMIT" as const,
        outcome: "TIME_LIMIT_EXCEEDED" as const,
        casesTotal: 0,
        casesPassed: 0,
        cases: [],
        error: { type: "TimeoutError", message: "The solution did not finish in time." },
      })),
    })
    click(/Submit/)

    await waitFor(() => expect(screen.getByText("Too slow")).toBeDefined())
  })

  it("says a broken runner is our problem, not their code's", async () => {
    panel({
      onSubmit: vi.fn(async () => ({
        problemSlug: "two-sum",
        kind: "SUBMIT" as const,
        outcome: "INTERNAL_ERROR" as const,
        casesTotal: 0,
        casesPassed: 0,
        cases: [],
        error: { type: "RunnerUnavailable", message: "That is a problem on our side, not with your code." },
      })),
    })
    click(/Submit/)

    await waitFor(() => expect(screen.getByText(/problem on our side/)).toBeDefined())
    // It must not read as a verdict on the learner's work.
    expect(screen.queryByText("Wrong answer")).toBeNull()
  })
})

describe("hints", () => {
  const hint: HintSelection = {
    level: 1,
    content: "Check what you store, and when.",
    stage: "CODING",
    trigger: "WRONG_ANSWER",
  }

  it("asks the server rather than choosing a rung locally", async () => {
    const onHint = vi.fn(async () => hint)
    panel({ onHint })
    click(/Hint/)

    await waitFor(() => expect(onHint).toHaveBeenCalledWith("CODING"))
    expect(screen.getByText("Check what you store, and when.")).toBeDefined()
  })

  it("tells the server which rung was read, so needing help is recorded", async () => {
    const onHintRead = vi.fn()
    panel({ onHint: vi.fn(async () => hint), onHintRead })
    click(/Hint/)

    await waitFor(() => expect(onHintRead).toHaveBeenCalledWith(1))
  })

  it("shows one rung at a time, never a list", async () => {
    panel({ onHint: vi.fn(async () => hint) })
    click(/Hint/)

    await waitFor(() => expect(screen.getByText("Check what you store, and when.")).toBeDefined())
    // Three hints stacked vertically is an explanation screen wearing a hint's clothes.
    expect(screen.getAllByRole("button", { name: /Dismiss this hint/ })).toHaveLength(1)
  })

  it("lets a hint be dismissed without it being un-read", async () => {
    const onHintRead = vi.fn()
    panel({ onHint: vi.fn(async () => hint), onHintRead })
    click(/Hint/)

    await waitFor(() => expect(screen.getByText("Check what you store, and when.")).toBeDefined())
    click(/Dismiss this hint/)

    await waitFor(() => expect(screen.queryByText("Check what you store, and when.")).toBeNull())
    // Dismissing hides it; the record that it was used is not a thing the learner can undo by
    // closing a panel, because it was already true.
    expect(onHintRead).toHaveBeenCalledTimes(1)
  })
})

describe("the reference solution", () => {
  it("is behind an explicit action, not shown next to the editor", () => {
    panel()
    expect(screen.getByText("Show me the reference solution")).toBeDefined()
    expect(screen.queryByRole("heading", { name: "Reference solution" })).toBeNull()
  })

  it("asks for it on click and shows it read-only", async () => {
    const onReveal = vi.fn(async (): Promise<RevealedSolution> => ({
      problemSlug: "two-sum",
      language: "PYTHON",
      code: "def two_sum(nums, target):\n    return [0, 1]",
      hintsUsed: 0,
    }))
    panel({ onReveal })
    click(/Show me the reference solution/)

    await waitFor(() =>
      expect(screen.getByRole("heading", { name: "Reference solution" })).toBeDefined(),
    )
    const shown = screen.getByLabelText("Reference solution, read only") as HTMLTextAreaElement
    expect(shown.value).toContain("def two_sum")
    // Read only, because copying is the point and editing it would be pointless.
    expect(shown.readOnly).toBe(true)
  })
})

describe("robustness", () => {
  it("says a failed request did not arrive instead of implying the code failed", async () => {
    panel({ onRun: vi.fn(async () => { throw new Error("Cannot reach PatternRun right now.") }) })
    click(/Run/)

    await waitFor(() =>
      expect(screen.getByRole("alert").textContent).toContain("Cannot reach PatternRun"),
    )
    // The distinction matters: a request that never left is not a verdict on the learner's code.
    expect(screen.queryByText("Wrong answer")).toBeNull()
  })

  /**
   * A runner failure must not also claim the code failed on every example.
   *
   * The banner says it is our problem; a list saying the code raised on all of them says the
   * opposite. A learner reads the list.
   */
  it("shows nothing per-case when nothing was evaluated", async () => {
    const { container } = panel({
      onRun: vi.fn(async () => ({
        problemSlug: "two-sum",
        kind: "RUN" as const,
        outcome: "INTERNAL_ERROR" as const,
        casesTotal: 0,
        casesPassed: 0,
        cases: [],
        error: { type: "RunnerUnavailable", message: "That is a problem on our side." },
      })),
    })
    click(/Run/)

    await waitFor(() => expect(screen.getByText(/problem on our side/)).toBeDefined())
    expect(container.textContent).not.toContain("it raised before returning")
    expect(container.textContent).not.toContain("hidden cases passed")
    expect(container.textContent).not.toContain("Example 1")
  })

  it("keeps the editor usable after a failure", async () => {
    panel({ onRun: vi.fn(async () => { throw new Error("offline") }) })
    const editor = screen.getByLabelText("Your solution")
    fireEvent.change(editor, { target: { value: "def two_sum(n, t): return [0, 1]" } })
    click(/Run/)

    await waitFor(() => expect(screen.getByRole("alert")).toBeDefined())
    expect((screen.getByLabelText("Your solution") as HTMLTextAreaElement).value).toBe(
      "def two_sum(n, t): return [0, 1]",
    )
  })

  it("restores saved source when the page is reloaded", async () => {
    const saved = "def two_sum(nums, target):\n    seen = {}\n    return [0, 1]"
    panel({ loadState: vi.fn(async () => ({ accepted: false, hintsUsed: 0, code: saved })) })

    await waitFor(() =>
      expect((screen.getByLabelText("Your solution") as HTMLTextAreaElement).value).toBe(saved),
    )
  })

  it("survives a load that fails, rather than blocking the editor", async () => {
    panel({ loadState: vi.fn(async () => { throw new Error("gone") }) })

    // An autosave that cannot restore is not worth an error screen over.
    await waitFor(() => expect(screen.getByRole("button", { name: /Run/ })).toBeDefined())
    fireEvent.change(screen.getByLabelText("Your solution"), {
      target: { value: "def two_sum(n, t): pass" },
    })
    expect((screen.getByLabelText("Your solution") as HTMLTextAreaElement).value).toBe(
      "def two_sum(n, t): pass",
    )
  })

  it("autosaves after a pause in typing", async () => {
    vi.useFakeTimers()
    try {
      const onSave = vi.fn()
      panel({ onSave })
      fireEvent.change(screen.getByLabelText("Your solution"), {
        target: { value: "def two_sum(n, t): pass" },
      })

      expect(onSave).not.toHaveBeenCalled()
      vi.advanceTimersByTime(2000)
      expect(onSave).toHaveBeenCalledWith("PYTHON", "def two_sum(n, t): pass")
    } finally {
      vi.useRealTimers()
    }
  })
})