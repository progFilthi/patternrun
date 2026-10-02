/**
 * The training loop (README section 60). The order is the loop: one phase at a time, and a
 * phase cannot be skipped.
 */
export const PHASES = [
  { id: "SCOUT", label: "Read" },
  { id: "PATTERN_GUESS", label: "Identify" },
  { id: "ANIMATION", label: "Animate" },
  { id: "HINTS", label: "Hint" },
  { id: "EXPLANATION", label: "Explain" },
  { id: "COMPLEXITY", label: "Complexity" },
  { id: "COMPLETE", label: "Finish" },
] as const

export type PhaseId = (typeof PHASES)[number]["id"]

export const FIRST_PHASE = 0
export const LAST_PHASE = PHASES.length - 1

/** Keeps an index inside the loop, so a bad index can never point at a phase that is not there. */
export function clampPhaseIndex(index: number): number {
  if (!Number.isFinite(index)) return FIRST_PHASE
  return Math.min(Math.max(Math.trunc(index), FIRST_PHASE), LAST_PHASE)
}

/**
 * Whether the stepper may offer a phase as a link.
 *
 * Reaching a phase is the only thing that unlocks it, and the sole way forward is a Continue
 * button that stays disabled until its gate is met. So the set of phases already visited is
 * exactly the set that was earned, and "reached" doubles as the permission to go back. Nothing
 * here ever lets the learner skip into a phase they have not earned, which is what keeps the
 * recall in Scout and Decode from being bypassed.
 */
export function isReachable(index: number, furthest: number): boolean {
  return index <= furthest
}

/**
 * The new furthest phase after moving to `target`.
 *
 * Moving back must not shrink the reachable set: going from Explain to Read to reread the
 * problem should not lock Animate again on the way forward. The furthest index ever reached is
 * therefore monotonic, and it is the ceiling the stepper renders.
 */
export function reachThrough(furthest: number, target: number): number {
  return Math.max(furthest, clampPhaseIndex(target))
}