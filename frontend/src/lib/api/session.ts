import type {
  AttemptCodeState,
  AttemptMode,
  AttemptResponse,
  AuthResponse,
  BreakdownEvaluation,
  BreakdownPrompt,
  CompletionResult,
  ExecutionResult,
  HintSelection,
  HintStage,
  RevealedSolution,
  ReviewOutcome,
} from "@/types/api"

/**
 * The browser side of the Phase 3 API.
 *
 * Two rules hold this module together.
 *
 * <p><b>Requests go to this origin.</b> URLs are relative, so they are served by the Next.js
 * rewrite that proxies `/api/v1` to the API. The browser therefore sees one origin, the session
 * cookie is same-site, and it is attached without any credentialed-CORS arrangement.
 *
 * <p><b>Only choices are sent.</b> Every request below carries what the learner picked or did:
 * a pattern slug, a hint level, a chosen option index, a complexity string, a source file. None of
 * them carry XP, an award, a combo, a mastery score, a correctness flag, or a claim that an answer
 * was right. Those arrive in {@link CompletionResult} and {@link ExecutionResult} as facts from
 * the backend, which is the only thing that decides them.
 */

const BASE_PATH = "/api/v1"

/** Why a request failed, in terms the UI can act on rather than a bare status code. */
export type ApiFailureKind =
  | "offline"
  | "session"
  | "conflict"
  | "rate-limited"
  | "rejected"
  | "server"

export class SessionApiError extends Error {
  readonly kind: ApiFailureKind
  readonly status: number

  constructor(kind: ApiFailureKind, status: number, message: string) {
    super(message)
    this.name = "SessionApiError"
    this.kind = kind
    this.status = status
  }

  /** Whether trying the same request again could plausibly succeed. */
  get isRetryable(): boolean {
    return this.kind === "offline" || this.kind === "server" || this.kind === "rate-limited"
  }
}

function kindFor(status: number): ApiFailureKind {
  if (status === 401) return "session"
  if (status === 409) return "conflict"
  if (status === 429) return "rate-limited"
  if (status >= 500) return "server"
  return "rejected"
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${BASE_PATH}${path}`, {
      ...init,
      // Same-origin, so the session cookie rides along automatically.
      credentials: "include",
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...init?.headers,
      },
    })
  } catch {
    // A sleeping or unreachable API is a normal state, not an exception to crash on.
    throw new SessionApiError(
      "offline",
      0,
      "Cannot reach PatternRun right now. Your session is safe.",
    )
  }

  if (!response.ok) {
    throw new SessionApiError(
      kindFor(response.status),
      response.status,
      await messageOf(response),
    )
  }

  // 204 has no body.
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

async function messageOf(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string; error?: string }
    return body.message ?? body.error ?? `Request failed (${response.status})`
  } catch {
    return `Request failed (${response.status})`
  }
}

function post<T>(path: string, body?: unknown): Promise<T> {
  return send<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) })
}

/** The learner's IANA zone, so the server can bucket days in it rather than trusting a date. */
function browserTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone
  } catch {
    return "UTC"
  }
}

/**
 * Establishes a session, creating an anonymous learner if there is none.
 *
 * Called once on load. There is no sign-up wall: an anonymous learner is a real learner, and
 * registering later claims the same row rather than replacing it, so nothing has to migrate.
 *
 * The zone is sent as a header rather than trusted as a day, because the server buckets streak
 * days itself and a client that could name its own day could rewrite its own history.
 *
 * The in-flight promise is module level on purpose. React's development double-invocation of
 * effects would otherwise issue two bootstraps, and the second can land before the first has
 * set its cookie, leaving an orphaned anonymous learner behind on every page load. Sharing the
 * promise makes a second caller wait for the first rather than start a competing identity.
 */
let bootstrapped: Promise<AuthResponse> | null = null

export function ensureSession(): Promise<AuthResponse> {
  if (!bootstrapped) {
    bootstrapped = send<AuthResponse>("/auth/session", {
      method: "POST",
      headers: { "X-Timezone": browserTimezone() },
    }).catch((error: unknown) => {
      // A failed bootstrap must not be cached, or the retry can never happen.
      bootstrapped = null
      throw error
    })
  }
  return bootstrapped
}

/** Test seam: forgets the shared bootstrap so a case can start from no session. */
export function resetSessionBootstrap(): void {
  bootstrapped = null
}

/** Opens a training session. Idempotent server-side: a refresh resumes rather than failing. */
export function startAttempt(problemSlug: string, mode: AttemptMode = "STANDARD") {
  return post<AttemptResponse>("/attempts", { problemSlug, mode })
}

/** The pattern the learner committed to, before the answer was revealed. */
export function recordPattern(attemptId: string, patternSlug: string) {
  return post<AttemptResponse>(`/attempts/${attemptId}/pattern`, { patternSlug })
}

/** One rung of the hint ladder. */
export function recordHint(attemptId: string, level: number) {
  return post<AttemptResponse>(`/attempts/${attemptId}/hint`, { level })
}

/**
 * An answer to a predict-the-move question.
 *
 * Only the chosen index travels. Whether it was right is worked out by the backend against the
 * animation step's own answer, because that answer already reached this browser.
 */
export function recordPrediction(attemptId: string, stepOrder: number, chosenIndex: number) {
  return post<AttemptResponse>(`/attempts/${attemptId}/predict`, { stepOrder, chosenIndex })
}

/**
 * Finishes the session.
 *
 * The payload is observations only: the two complexity strings the learner picked, the result
 * of the breakdown check, how long it took, and the predictions as chosen. No XP, no awards, no
 * correctness flags.
 *
 * Safe to retry: the backend treats a repeat completion as a replay of the recorded result.
 */
export function completeAttempt(
  attemptId: string,
  result: {
    complexityTime: string
    complexitySpace: string
    durationMs: number
    predictions: { stepOrder: number; chosenIndex: number }[]
  },
) {
  return post<CompletionResult>(`/attempts/${attemptId}/complete`, result)
}

/**
 * Submits what the learner chose for each breakdown prompt.
 *
 * Keys and chosen indices only. The read that supplies the prompts deliberately omits the
 * answer key, so the verdict has to come back from the server.
 */
export function submitBreakdown(
  attemptId: string,
  answers: { key: string; chosenIndex: number }[],
) {
  return post<BreakdownEvaluation>(`/attempts/${attemptId}/breakdown`, { answers })
}

export function logout() {
  return post<void>("/auth/logout")
}

/**
 * Reviews one mistake.
 *
 * The only field is the learner's own judgement of whether they now have it. That is a claim about
 * their understanding rather than something the backend can verify, and it is not treated as one: it
 * decides the review interval and nothing else. XP, the review count and the next due date come
 * back as the server computed them.
 *
 * The backend refuses a review that is not due yet, so this cannot be called twice to collect the
 * award twice.
 */
export function reviewMistake(mistakeId: string, correct: boolean) {
  return post<ReviewOutcome>(`/progress/review/${mistakeId}`, { correct })
}
/**
 * The breakdown prompts for a problem.
 *
 * Read through the same-origin proxy like every other browser call. The response carries no
 * answer index and no explanation, so the options cannot be matched against a key that was
 * simply shipped down with them.
 */
export async function fetchBreakdown(slug: string): Promise<BreakdownPrompt[]> {
  const response = await send<{ problemSlug: string; prompts: BreakdownPrompt[] }>(
    `/problems/${slug}/breakdown`,
  )
  return response.prompts
}

/*
 * Phase 4: the coding stage.
 *
 * Every function below sends a language and a source string, and nothing else. There is no
 * `passed`, no `outcome`, no `accepted` in anything this module can put on the wire, which is the
 * point: the backend runs the code and returns what it found. A learner who wants to convince the
 * server their solution works has to make it work.
 */

/**
 * Checks the current code against the visible examples.
 *
 * Two request bodies, one record. They are separate functions rather than one with a flag so that
 * no caller can reach Submit by passing an argument, and so the difference between checking your
 * work and being done with a problem is visible in the code that calls it.
 */
export function runCode(attemptId: string, language: string, code: string) {
  return post<ExecutionResult>(`/attempts/${attemptId}/code/run`, { language, code })
}

/** Evaluates against the backend-controlled case set. The only path that can accept a solution. */
export function submitCode(attemptId: string, language: string, code: string) {
  return post<ExecutionResult>(`/attempts/${attemptId}/code/submit`, { language, code })
}

/**
 * Autosave.
 *
 * Decides nothing and returns no verdict, which is why it cannot be mistaken for a Run. It exists
 * so a refresh does not cost the learner what they had written.
 */
export function saveCode(attemptId: string, language: string, code: string) {
  return post<AttemptCodeState>(`/attempts/${attemptId}/code/save`, { language, code })
}

/** The saved source and the last verdict, so a reload can restore what was being written. */
export function fetchCodeState(attemptId: string): Promise<AttemptCodeState> {
  return send<AttemptCodeState>(`/attempts/${attemptId}/code`)
}

/**
 * The next hint for the stage the learner is in.
 *
 * Only the stage travels. The trigger is decided by the backend from the last execution it
 * observed, because a client that could name its own trigger could ask for the debugging ladder
 * after a passing submission and walk to the answer without ever having failed.
 */
export function fetchNextHint(
  attemptId: string,
  stage: HintStage = "CODING",
): Promise<HintSelection | null> {
  return send<HintSelection | null>(`/attempts/${attemptId}/hint/next?stage=${stage}`)
}

/**
 * The reference implementation.
 *
 * A POST on purpose. Fetching the answer is an event worth recording, so making it a read would
 * mean there was either no record or a GET that hands the solution to anyone who guessed the URL.
 */
export function revealSolution(
  attemptId: string,
  language = "PYTHON",
): Promise<RevealedSolution> {
  return post<RevealedSolution>(`/attempts/${attemptId}/solution?language=${language}`)
}
