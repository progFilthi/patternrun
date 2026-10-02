import { readFileSync, readdirSync } from "node:fs"
import { join } from "node:path"

import { describe, expect, it } from "vitest"

import { COMPLEXITY_LADDER, isCorrectChoice, normalize, optionsFor } from "@/lib/training/complexity"

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

  it("folds the spaces around a multiplication sign", () => {
    expect(isCorrectChoice("O(m * n)", "O(m*n)")).toBe(true)
    expect(isCorrectChoice("O(m*n)", "O(m * n)")).toBe(true)
  })

  it("does not reorder operands, because it cannot know that a*b and b*a are equal", () => {
    // Deliberate. The options are derived from the problem's own answer, so the rendered button
    // is byte-identical to what is compared and order never arises. Reordering would mean parsing
    // a product, which is more cleverness than this rule earns.
    expect(normalize("O(n * m)")).not.toBe(normalize("O(m * n)"))
  })
})

describe("optionsFor", () => {
  /**
   * The bug this exists to make impossible.
   *
   * Five of the twenty seeded problems published a complexity the fixed ladder could not offer, so
   * those learners were shown "wrong" no matter what they picked. Correct code marked as incorrect.
   * Asserted against the real seeded values rather than the ones remembered, because the whole
   * failure was that nobody cross-checked the list against the content.
   */
  const SEEDED = [
    { time: "O(n log k)", space: "O(k)" }, // kth-largest-element-in-an-array
    { time: "O(n + m)", space: "O(1)" }, // minimum-window-substring
    { time: "O(m * n)", space: "O(m * n)" }, // number-of-islands, rotting-oranges
    { time: "O(n log k)", space: "O(n)" }, // top-k-frequent-elements
    { time: "O(n^2)", space: "O(1)" }, // 3sum, caret spelling
    { time: "O(n)", space: "O(n)" }, // two-sum
  ]

  it.each(SEEDED)("always offers the correct answer for $time / $space", (complexity) => {
    const options = optionsFor(complexity)

    expect(options.some((option) => isCorrectChoice(option, complexity.time))).toBe(true)
    expect(options.some((option) => isCorrectChoice(option, complexity.space))).toBe(true)
  })

  it("keeps the general ladder available, so the step is still a choice", () => {
    const options = optionsFor({ time: "O(m * n)", space: "O(m * n)" })

    for (const rung of COMPLEXITY_LADDER) {
      expect(options).toContain(rung)
    }
  })

  it("never offers the same answer twice in two spellings", () => {
    const options = optionsFor({ time: "O(n^2)", space: "O(1)" })
    const forms = options.map(normalize)

    expect(new Set(forms).size).toBe(forms.length)
    // O(n²) is already on the ladder, so the caret answer must fold into it rather than join it.
    expect(options.filter((option) => normalize(option) === "o(n2)")).toHaveLength(1)
  })

  it("does not invent options for a problem that needs none", () => {
    expect(optionsFor({ time: "O(n)", space: "O(1)" })).toEqual(["O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n²)"])
  })
})
/**
 * The same guarantee, checked against every problem that actually ships.
 *
 * The list above is a hand copy, which is precisely how the original bug survived: nobody
 * cross-checked the catalogue against the content. Reading the seed files means a content change
 * that publishes an unselectable complexity fails here instead of silently stranding a learner on
 * a step they cannot pass.
 */
describe("every shipped problem can be answered", () => {
  const seedDir = join(
    process.cwd(),
    "..",
    "backend",
    "src",
    "main",
    "resources",
    "seed",
    "problems",
  )

  const shipped = readdirSync(seedDir)
    .filter((file) => file.endsWith(".json"))
    .map((file) => JSON.parse(readFileSync(join(seedDir, file), "utf8")) as {
      slug: string
      timeComplexity: string
      spaceComplexity: string
    })

  it("found the seed content to check", () => {
    expect(shipped.length).toBeGreaterThan(0)
  })

  it.each(shipped)("$slug offers its own time complexity", (problem) => {
    const options = optionsFor({
      time: problem.timeComplexity,
      space: problem.spaceComplexity,
    })
    expect(options.some((option) => isCorrectChoice(option, problem.timeComplexity))).toBe(true)
  })

  it.each(shipped)("$slug offers its own space complexity", (problem) => {
    const options = optionsFor({
      time: problem.timeComplexity,
      space: problem.spaceComplexity,
    })
    expect(options.some((option) => isCorrectChoice(option, problem.spaceComplexity))).toBe(true)
  })
})
