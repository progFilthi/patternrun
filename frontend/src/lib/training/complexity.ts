/** Complexity options for the check step (README section 11). */
export const COMPLEXITY_OPTIONS = ["O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n²)"] as const

export type ComplexityChoice = (typeof COMPLEXITY_OPTIONS)[number]

export function isCorrectChoice(choice: string | null, expected: string): boolean {
  if (!choice) return false
  return normalize(choice) === normalize(expected)
}

/** Treats "O(n²)" and "O(n^2)" as the same answer. */
function normalize(value: string): string {
  return value.trim().toLowerCase().replace("^", "")
}