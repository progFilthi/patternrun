import { describe, expect, it } from "vitest"

import { COMPLEXITY_OPTIONS, isCorrectChoice } from "@/lib/training/complexity"

describe("isCorrectChoice", () => {
  it("accepts an exact answer", () => {
    expect(isCorrectChoice("O(n)", "O(n)")).toBe(true)
    expect(isCorrectChoice("O(1)", "O(1)")).toBe(true)
    expect(isCorrectChoice("O(log n)", "O(log n)")).toBe(true)
  })

  it("folds the superscript and the caret to the same answer", () => {
    // The regression that mattered. The options are written with superscripts and the seeded
    // answers with carets, so folding only the caret left any quadratic problem with no
    // reachable correct answer: the learner picked the right one and was told they were wrong.
    expect(isCorrectChoice("O(n²)", "O(n^2)")).toBe(true)
    expect(isCorrectChoice("O(n^2)", "O(n²)")).toBe(true)
    expect(isCorrectChoice("O(n³)", "O(n^3)")).toBe(true)
  })

  it("ignores case and surrounding space", () => {
    expect(isCorrectChoice("  o(n) ", "O(n)")).toBe(true)
    expect(isCorrectChoice("O(LOG N)", "O(log n)")).toBe(true)
  })

  it("keeps genuinely different answers apart", () => {
    expect(isCorrectChoice("O(n)", "O(n log n)")).toBe(false)
    expect(isCorrectChoice("O(1)", "O(n)")).toBe(false)
    expect(isCorrectChoice("O(n + m)", "O(n)")).toBe(false)
  })

  it("never treats an absent answer as right", () => {
    expect(isCorrectChoice(null, "O(n)")).toBe(false)
    expect(isCorrectChoice("", "O(n)")).toBe(false)
  })

  it("matches every quadratic option against a caret answer", () => {
    // 3sum is seeded with O(n^2), and O(n²) is the only quadratic option offered.
    const quadratic = COMPLEXITY_OPTIONS.filter((option) => option.includes("²"))
    expect(quadratic).toHaveLength(1)
    expect(isCorrectChoice(quadratic[0], "O(n^2)")).toBe(true)
  })
})