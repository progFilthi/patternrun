/** Complexity options for the check step (README section 11). */
export const COMPLEXITY_OPTIONS = ["O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n²)"] as const

export type ComplexityChoice = (typeof COMPLEXITY_OPTIONS)[number]

export function isCorrectChoice(choice: string | null, expected: string): boolean {
  if (!choice) return false
  return normalize(choice) === normalize(expected)
}

/**
 * Folds both spellings of a complexity to one canonical form.
 *
 * The options are written with superscripts ("O(n²)") and the seeded answers with carets
 * ("O(n^2)"), so folding only the caret left the two unequal. That made the correct answer
 * unreachable on any quadratic problem: the learner picked the right one and was told they were
 * wrong, losing the complexity award and the correctness half of their mastery. The backend
 * applies the identical rule, so the two can never disagree about what "correct" means.
 */
function normalize(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/\u00b2/g, "2")
    .replace(/\u00b3/g, "3")
    .replace("^", "")
}