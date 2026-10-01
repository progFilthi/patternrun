/**
 * Types mirroring the Phase 1 API contracts. Field names and shapes are taken from the
 * running backend, not invented. See docs/architecture.md for the endpoint list.
 */

export type Difficulty = "EASY" | "MEDIUM" | "HARD"

export type TrainingDifficulty =
  | "RECOGNITION"
  | "GUIDED"
  | "INDEPENDENT"
  | "SPEEDRUN"
  | "BOSS"

/** Animation step types supported by the renderer (README section 29). */
export type AnimationStepType =
  | "ARRAY"
  | "POINTER"
  | "WINDOW"
  | "HASH_MAP"
  | "STACK"
  | "HEAP"
  | "TREE"
  | "GRAPH"
  | "GRID"
  | "INTERVAL"
  | "PREFIX_SUM"
  | "DP_TABLE"
  | "CODE"
  | "TEXT"
  | "QUESTION"
  | "SUCCESS"
  | "FAILURE"

export interface PatternRef {
  id: string
  slug: string
  name: string
}

export interface PatternSummary {
  id: string
  slug: string
  name: string
  signal: string
  difficultyOrder: number
  problemCount: number
}

export interface PatternDetail {
  id: string
  slug: string
  name: string
  summary: string
  signal: string
  mentalModel: string
  recognitionRules: string[]
  template: string[]
  invariant: string
  difficultyOrder: number
  problemCount: number
}

export interface Complexity {
  time: string
  space: string
}

export interface ProblemSummary {
  id: string
  slug: string
  title: string
  externalId: number
  difficulty: Difficulty
  trainingDifficulty: TrainingDifficulty
  pattern: PatternRef
  complexity: Complexity
}

export interface ProblemExample {
  ordinal: number
  input: string
  output: string
  explanation: string
}

export interface ProblemDetail {
  id: string
  slug: string
  title: string
  externalId: number
  difficulty: Difficulty
  trainingDifficulty: TrainingDifficulty
  statement: string
  constraints: string[]
  examples: ProblemExample[]
  pattern: PatternRef
  secondaryPatterns: PatternRef[]
  pseudocode: string[]
  complexity: Complexity
  whyThisPattern: string
  bruteForce: string
  invariant: string
  interviewExplanation: string
  commonMistakes: string[]
  hintCount: number
  animationStepCount: number
  visibleTestCaseCount: number
}

export interface Hint {
  level: number
  content: string
}

/** Payload is free form JSON by design (README section 28). */
export type AnimationPayload = Record<string, unknown>

export interface AnimationStep {
  order: number
  type: AnimationStepType
  title: string
  description: string
  /** Text alternative for every step; the animation must be understandable without motion. */
  text: string
  payload: AnimationPayload
}

export interface TestCase {
  ordinal: number
  label: string
  input: string
  expectedOutput: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ApiError {
  status: number
  error: string
  message: string
  path: string
  timestamp: string
}