/**
 * History recovered from the browser, and now read only.
 *
 * In Phase 2 this module was the store: sessions were written here because the API had no
 * write endpoints. Phase 3 made the backend the record, so nothing writes this key any more.
 * It is kept because the blob in a returning learner's browser predates their account, and
 * importing it is how that history reaches the server instead of being silently lost.
 *
 * Reading is therefore the live path and writing is dead. Kept intact rather than trimmed
 * because the shape of {@link CompletionRecord} is the contract the import has to satisfy.
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