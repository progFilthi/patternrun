import type {
  AnimationStep,
  ApiError,
  Hint,
  PageResponse,
  PatternDetail,
  PatternSummary,
  ProblemDetail,
  ProblemSummary,
  TestCase,
} from "@/types/api"

/**
 * Server side client. The API is the single source of truth for content; this module only
 * talks to it. `API_URL` is used on the server, `NEXT_PUBLIC_API_URL` when the browser has to
 * reach the API directly (only used by the keep the session alive probe).
 */
const serverUrl = process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
const browserUrl = process.env.NEXT_PUBLIC_API_URL ?? serverUrl

export const API_BASE_PATH = "/api/v1"

function apiUrl(path: string): string {
  return `${serverUrl.replace(/\/$/, "")}${API_BASE_PATH}${path}`
}

export class ApiRequestError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = "ApiRequestError"
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(apiUrl(path), {
      ...init,
      headers: { Accept: "application/json", ...init?.headers },
      next: { revalidate: 300 },
    })
  } catch {
    // A sleeping free tier API is a normal state, not an exception to crash on.
    throw new ApiRequestError(503, "The API is not reachable right now.")
  }

  if (!response.ok) {
    throw new ApiRequestError(response.status, await errorMessage(response))
  }
  return (await response.json()) as T
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ApiError
    return body.message || body.error
  } catch {
    return `Request failed with status ${response.status}`
  }
}

export const api = {
  listPatterns(): Promise<PatternSummary[]> {
    return request<PatternSummary[]>("/patterns")
  },

  getPattern(slug: string): Promise<PatternDetail> {
    return request<PatternDetail>(`/patterns/${slug}`)
  },

  listPatternProblems(slug: string): Promise<ProblemSummary[]> {
    return request<ProblemSummary[]>(`/patterns/${slug}/problems`)
  },

  listProblems(options?: { pattern?: string; size?: number }): Promise<PageResponse<ProblemSummary>> {
    const params = new URLSearchParams()
    if (options?.pattern) params.set("pattern", options.pattern)
    params.set("size", String(options?.size ?? 100))
    return request<PageResponse<ProblemSummary>>(`/problems?${params.toString()}`)
  },

  getProblem(slug: string): Promise<ProblemDetail> {
    return request<ProblemDetail>(`/problems/${slug}`)
  },

  getHints(slug: string): Promise<Hint[]> {
    return request<Hint[]>(`/problems/${slug}/hints`)
  },

  getAnimation(slug: string): Promise<AnimationStep[]> {
    return request<AnimationStep[]>(`/problems/${slug}/animation`)
  },

  getTestCases(slug: string): Promise<TestCase[]> {
    return request<TestCase[]>(`/problems/${slug}/test-cases`)
  },
}

/** Used by the client to check whether the API is awake before starting a session. */
export function browserApiUrl(path: string): string {
  return `${browserUrl.replace(/\/$/, "")}${API_BASE_PATH}${path}`
}