import { describe, expect, it } from "vitest"

import {
  FIRST_PHASE,
  LAST_PHASE,
  PHASES,
  clampPhaseIndex,
  isReachable,
  reachThrough,
} from "@/lib/training/phases"

describe("PHASES", () => {
  it("is the eight phase loop in order, with coding between complexity and finishing", () => {
    expect(PHASES.map((phase) => phase.id)).toEqual([
      "SCOUT",
      "PATTERN_GUESS",
      "ANIMATION",
      "HINTS",
      "EXPLANATION",
      "COMPLEXITY",
      "CODE",
      "COMPLETE",
    ])
  })

  /**
   * The ordering is a teaching decision, so it is asserted rather than left to the array.
   *
   * Complexity is the last thing reasoned about before writing, so the editor has to follow it
   * directly. Putting CODE before COMPLEXITY would ask for a complexity guess before there was
   * anything to have guessed about, and putting it after COMPLETE would make it an afterthought
   * rather than the part of the loop that tests the understanding.
   */
  it("puts the editor after the reasoning and before the finish", () => {
    const ids = PHASES.map((phase) => phase.id)
    expect(ids.indexOf("CODE")).toBe(ids.indexOf("COMPLEXITY") + 1)
    expect(ids.indexOf("CODE")).toBe(ids.indexOf("COMPLETE") - 1)
  })

  it("gives every phase a label and a unique id", () => {
    const ids = PHASES.map((phase) => phase.id)
    expect(new Set(ids).size).toBe(ids.length)
    for (const phase of PHASES) {
      expect(phase.label.length).toBeGreaterThan(0)
    }
  })

  it("exposes the loop bounds", () => {
    expect(FIRST_PHASE).toBe(0)
    expect(LAST_PHASE).toBe(PHASES.length - 1)
  })
})

describe("clampPhaseIndex", () => {
  it("passes an in-range index through", () => {
    expect(clampPhaseIndex(0)).toBe(0)
    expect(clampPhaseIndex(3)).toBe(3)
    expect(clampPhaseIndex(LAST_PHASE)).toBe(LAST_PHASE)
  })

  it("keeps an index inside the loop", () => {
    expect(clampPhaseIndex(-1)).toBe(FIRST_PHASE)
    expect(clampPhaseIndex(-999)).toBe(FIRST_PHASE)
    expect(clampPhaseIndex(LAST_PHASE + 1)).toBe(LAST_PHASE)
    expect(clampPhaseIndex(999)).toBe(LAST_PHASE)
  })

  it("falls back to the first phase for a value that is not a number", () => {
    expect(clampPhaseIndex(Number.NaN)).toBe(FIRST_PHASE)
    expect(clampPhaseIndex(Number.POSITIVE_INFINITY)).toBe(FIRST_PHASE)
    expect(clampPhaseIndex(Number.NEGATIVE_INFINITY)).toBe(FIRST_PHASE)
  })

  it("truncates a fractional index rather than pointing between phases", () => {
    expect(clampPhaseIndex(2.9)).toBe(2)
    expect(clampPhaseIndex(-0.5)).toBe(0)
  })

  it("never returns an index with no phase behind it", () => {
    for (const candidate of [-5, -0.2, 0, 1.5, 6, 6.1, 42, Number.NaN]) {
      expect(clampPhaseIndex(candidate)).toBeGreaterThanOrEqual(FIRST_PHASE)
      expect(clampPhaseIndex(candidate)).toBeLessThanOrEqual(LAST_PHASE)
    }
  })
})

describe("isReachable", () => {
  it("offers every phase up to the furthest one", () => {
    const flags = PHASES.map((_, index) => isReachable(index, 2))
    expect(flags).toEqual([true, true, true, false, false, false, false, false])
  })

  it("locks everything past the furthest phase", () => {
    expect(isReachable(FIRST_PHASE + 1, FIRST_PHASE)).toBe(false)
    expect(isReachable(LAST_PHASE, LAST_PHASE - 1)).toBe(false)
  })

  it("keeps the current phase reachable even at the very start", () => {
    expect(isReachable(FIRST_PHASE, FIRST_PHASE)).toBe(true)
  })

  it("does not unlock ahead when the furthest phase moves backwards", () => {
    // Going back to Read must not shrink what the stepper offers.
    const furthest = 4
    const visitedAfterGoingBack = [4, 3, 2, 1, 0]
    for (const furthestNow of visitedAfterGoingBack) {
      expect(isReachable(LAST_PHASE, furthestNow)).toBe(false)
    }
    expect(isReachable(4, furthest)).toBe(true)
  })
})

describe("reachThrough", () => {
  it("raises the ceiling to a phase that was just reached", () => {
    expect(reachThrough(0, 3)).toBe(3)
    expect(reachThrough(2, 5)).toBe(5)
  })

  it("does not lower the ceiling when moving backwards", () => {
    // The regression this guards: reread Read, and Animate must still be clickable.
    expect(reachThrough(4, 0)).toBe(4)
    expect(reachThrough(LAST_PHASE, FIRST_PHASE)).toBe(LAST_PHASE)
  })

  it("keeps the ceiling when revisiting the current phase", () => {
    expect(reachThrough(3, 3)).toBe(3)
  })

  it("clamps a target outside the loop before using it", () => {
    expect(reachThrough(2, 99)).toBe(LAST_PHASE)
    expect(reachThrough(2, -99)).toBe(2)
  })

  it("is monotonic across a whole session, whatever order phases are visited in", () => {
    let furthest = FIRST_PHASE
    const visits = [1, 2, 3, 0, 4, 1, 6, 0, 5, 2, LAST_PHASE]

    for (const visit of visits) {
      const previous = furthest
      furthest = reachThrough(furthest, visit)
      expect(furthest).toBeGreaterThanOrEqual(previous)
      expect(furthest).toBeGreaterThanOrEqual(clampPhaseIndex(visit))
      expect(furthest).toBeLessThanOrEqual(LAST_PHASE)
    }

    expect(furthest).toBe(LAST_PHASE)
  })
})