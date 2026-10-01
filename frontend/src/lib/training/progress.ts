/**
 * Anonymous local progress. The API is read only in Phase 2 and there are no accounts, so a
 * completed session is recorded in localStorage only (README sections 73 and 74).
 */

export interface CompletionRecord {
  completedAt: string
  patternCorrect: boolean
  hintsUsed: number
  complexityCorrect: boolean
  predictionsAnswered: number
}

export interface LocalProgress {
  completions: Record<string, CompletionRecord>
}

const STORAGE_KEY = "patternrun.progress.v1"

const EMPTY: LocalProgress = { completions: {} }

export function readProgress(): LocalProgress {
  if (typeof window === "undefined") return EMPTY
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return EMPTY
    const parsed = JSON.parse(raw) as Partial<LocalProgress>
    return { completions: parsed.completions ?? {} }
  } catch {
    return EMPTY
  }
}

export function recordCompletion(slug: string, record: CompletionRecord): LocalProgress {
  const next: LocalProgress = {
    completions: { ...readProgress().completions, [slug]: record },
  }
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  } catch {
    // Private mode or a full quota must never break a session.
  }
  return next
}

export function isCompleted(slug: string): boolean {
  return readProgress().completions[slug] !== undefined
}

/** Small helper for list screens that need completion state. */
export function completedSlugs(): Set<string> {
  return new Set(Object.keys(readProgress().completions))
}