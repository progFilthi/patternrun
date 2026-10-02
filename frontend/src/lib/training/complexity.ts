import type { Complexity } from "@/types/api"

/**
 * The shapes a learner is offered at the complexity step.
 *
 * A general ladder rather than a per-problem list, because the point of the step is to make them
 * choose rather than read. What is *not* fixed is which options appear: see {@link optionsFor}.
 */
export const COMPLEXITY_LADDER = [
  "O(1)",
  "O(log n)",
  "O(n)",
  "O(n log n)",
  "O(n²)",
] as const

export type ComplexityChoice = (string) & {}

export function isCorrectChoice(choice: string | null, expected: string): boolean {
  if (!choice) return false
  return normalize(choice) === normalize(expected)
}

/**
 * The options for one problem: the ladder, plus whatever that problem's own answers are.
 *
 * The bug this fixes was worse than it looked. A hard-coded list can only offer complexities that
 * were known when it was written, so five of the twenty seeded problems published an answer the
 * learner could not select — `O(n log k)`, `O(k)`, `O(n + m)`, `O(m * n)`. Those learners were
 * shown "wrong" no matter what they picked, lost the complexity award, and forfeited the
 * complexity half of their mastery. Correct code, marked as incorrect, which is the worst thing this
 * step can do.
 *
 * Adding the problem's own answers to the ladder makes that class of bug impossible rather than
 * merely fixed: whatever complexity the next authored problem declares, its correct answer is
 * present by construction. Deduped by normalised form, so `O(n^2)` does not appear twice next to
 * `O(n²)`, and sorted so the general shapes stay in their familiar order with the specific ones
 * after.
 */
export function optionsFor(expected: Complexity): string[] {
  const seen = new Set<string>()
  const options: string[] = [];

  for (const candidate of [...COMPLEXITY_LADDER, expected.time, expected.space]) {
    const key = normalize(candidate);
    if (!key || seen.has(key)) continue;
    seen.add(key);
    options.push(candidate);
  }
  return options;
}

/**
 * Folds the spellings of a complexity to one canonical form.
 *
 * Options are written with superscripts (`O(n²)`) and seeded answers with carets (`O(n^2)`), so
 * folding only the caret left the two unequal. Multiplication is folded too, and whitespace removed
 * entirely, because `O(m * n)` and `O(m*n)` mean the same thing and a learner should not lose an
 * award over a space. The backend applies the identical rule, so the two can never disagree about
 * what "correct" means.
 *
 * Exported because a component needs the same authority to decide which option to highlight, and
 * two copies of this rule is how the two started disagreeing in the first place.
 */
export function normalize(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/\u00b2/g, "2")
    .replace(/\u00b3/g, "3")
    .replace(/\^/g, "")
    .replace(/\s*\*\s*/g, "*")
    .replace(/\s+/g, "");
}