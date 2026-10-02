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
  /**
   * The function the backend will call, or absent when this problem is not available for coding.
   *
   * Its absence is what stops the editor being offered for a problem whose tests cannot be run, so
   * the client never shows a control that is guaranteed to fail.
   */
  runnableEntrypoint?: string
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
/*
 * Phase 3: accounts, attempts and progress.
 *
 * Field names mirror the backend DTO records exactly. Nothing here computes XP, mastery, a
 * combo or whether an answer was right: those are decided server-side and arrive as facts.
 *
 * Nullable fields are optional rather than `| null` because the API is configured to omit nulls
 * (`spring.jackson.default-property-inclusion: non_null`), so an absent key is how "not
 * measured" arrives.
 */

export interface UserResponse {
  id: string
  email?: string
  username?: string
  anonymous: boolean
  timezone: string
  createdAt?: string
}

/** `sessionToken` is never serialised; its presence here would be a leak. */
export interface AuthResponse {
  user: UserResponse
  sessionExpiresAt?: string
}

export type AttemptMode = "STANDARD" | "SPEEDRUN" | "BOSS"
export type AttemptStatus = "IN_PROGRESS" | "COMPLETED" | "ABANDONED"

export interface AttemptResponse {
  id: string
  problemSlug: string
  mode: AttemptMode
  status: AttemptStatus
  hintsUsed: number
  patternCorrect: boolean
  predictionsAnswered: number
  predictionsCorrect: number
  complexityCorrect: boolean
  breakdownCorrect: boolean
  durationMs?: number
  completedAt?: string
}

/** One line of the XP ledger, already worded for a human by the backend. */
export interface XpAward {
  reason: string
  label: string
  xp: number
  alreadyEarned: boolean
}

/**
 * What one finished session earned, decided entirely by the backend.
 *
 * This is the only place the UI learns its numbers. `provisional` means the grade had to leave
 * part of its rubric unevidenced, which happens for an attempt with no timing.
 */
export interface CompletionResult {
  attemptId: string
  problemSlug: string
  grade: string
  provisional: boolean
  patternCorrect: boolean
  complexityCorrect: boolean
  breakdownCorrect: boolean
  predictionsAnswered: number
  predictionsCorrect: number
  hintsUsed: number
  durationMs?: number
  totalXpAwarded: number
  combo: number
  awards: XpAward[]
  patternSlug: string
  patternMastery?: number
  personalBest: boolean
  mistakesResolved: string[]
  /**
   * Phase 4. Absent means no code has been evaluated, which is not the same as "failed".
   *
   * These three are reported, not scored. Gating the grade on an accepted solution is a product
   * decision that should be made with the evidence visible, and Phase 3's awards are unchanged
   * while that is being decided.
   */
  codeOutcome?: ExecutionOutcome
  codeAccepted: boolean
  solvedIndependently: boolean
}

export interface PatternMastery {
  patternSlug: string
  patternName: string
  difficultyOrder: number
  /** Absent means "not measured yet", which is not the same as zero. */
  recognition?: number
  correctness?: number
  explanation?: number
  speed?: number
  retention?: number
  overall?: number
  attemptsCount: number
}

export interface DailyProgress {
  day: string
  xpEarned: number
  problemsCompleted: number
  patternsIdentified: number
  mistakesReviewed: number
  speedrunsCompleted: number
  activeMinutes: number
  goalMet: boolean
  questCompleted: boolean
  questItemsCompleted: number
}

export interface ProgressSummary {
  totalXp: number
  level: number
  levelName: string
  levelProgress: number
  streak: number
  longestStreak: number
  trainedToday: boolean
  today?: DailyProgress
  patterns: PatternMastery[]
  problemsCompleted: number
  openMistakes: number
}

export interface ProblemProgress {
  problemSlug: string
  attempts: number
  completed: boolean
  lastGrade?: string
  bestDurationMs?: number
  timesCompleted: number
}

/*
 * Phase 4: the coding stage.
 *
 * Everything here describes what the backend did after running the learner's code. The browser sent
 * a string and nothing else, so there is no field in this file the client could have set to make
 * itself pass.
 */

/**
 * How a run ended.
 *
 * The distinction between WRONG_ANSWER and RUNTIME_ERROR is the one that matters most and is
 * worth the enum's length: one says the idea did not hold, the other says the code never got far
 * enough to have an idea. A single "failed" would send someone looking for a logic bug in a
 * solution that has a typo.
 */
export type ExecutionOutcome =
  | "ACCEPTED"
  | "WRONG_ANSWER"
  | "RUNTIME_ERROR"
  | "TIME_LIMIT_EXCEEDED"
  | "MEMORY_LIMIT_EXCEEDED"
  | "SYNTAX_ERROR"
  | "INTERNAL_ERROR"

/**
 * Whether this was a check or an evaluation.
 *
 * Run is the learner testing against examples they were already shown; Submit is the backend's
 * hidden evaluation set. Only Submit can ever produce ACCEPTED.
 */
export type ExecutionKind = "RUN" | "SUBMIT"

/**
 * One case's result.
 *
 * A hidden case carries `passed` and nothing else — no label, input, expected value or output. The
 * keys are absent rather than null because the API omits nulls, so `actual: undefined` is a normal
 * shape here and not a missing response.
 */
export interface ExecutionCaseResult {
  ordinal: number
  label?: string
  input?: string
  expected?: string
  actual?: string
  passed: boolean
  hidden: boolean
}

/** A failure, already phrased for display. `line` is in the learner's own file. */
export interface ExecutionError {
  type: string
  line?: number
  message: string
}

export interface ExecutionResult {
  problemSlug: string
  kind: ExecutionKind
  outcome: ExecutionOutcome
  casesTotal: number
  casesPassed: number
  durationMs?: number
  cases: ExecutionCaseResult[]
  error?: ExecutionError
}

/** The saved source plus the last thing the server concluded about it. Survives a refresh. */
export interface AttemptCodeState {
  language?: string
  code?: string
  outcome?: ExecutionOutcome
  accepted: boolean
  hintsUsed: number
}

/** One rung, chosen for the moment the learner is in. */
export type HintStage = "BREAKDOWN" | "REASONING" | "CODING" | "ANY"

export type HintTrigger =
  | "ANY"
  | "WRONG_ANSWER"
  | "RUNTIME_ERROR"
  | "SYNTAX_ERROR"
  | "TIME_LIMIT_EXCEEDED"

export interface HintSelection {
  level: number
  content: string
  stage: HintStage
  trigger: HintTrigger
}

/** One mistake that is actually due for review. */
export interface ReviewItem {
  mistakeId: string
  problemSlug: string
  problemTitle: string
  category: MistakeCategory
  description: string
  lesson: string
  reviewCount: number
  intervalDays: number
  dueAt: string
  attemptsSince: number
}

export type MistakeCategory = "PATTERN" | "LOGIC" | "TESTS" | "COMPLEXITY" | "EXPLANATION"

/**
 * The learner's judgement about a mistake they are reviewing.
 *
 * One boolean, and it is the only field. It decides the review interval and nothing else: XP, the
 * review count and the next due date are server-owned, and there is nowhere in this shape for a
 * client to put them.
 */
export interface ReviewRequest {
  correct: boolean
}

/** What reviewing one mistake did. Interval and next due date are the server's answer. */
export interface ReviewOutcome {
  mistakeId: string
  problemSlug: string
  category: MistakeCategory
  correct: boolean
  reviewCount: number
  intervalDays: number
  nextDueAt: string
  xpAwarded: number
  /** True when this review earned nothing because it had already been paid for. */
  alreadyEarned: boolean
}

/** The reference implementation. The last escape hatch, and it is always recorded as taken. */
export interface RevealedSolution {
  problemSlug: string
  language: string
  code: string
  hintsUsed: number
}
/** Phase 3 breakdown: reading the problem before choosing a tool. */
export interface BreakdownPrompt {
  key: string
  prompt: string
  options: string[]
}

/** The verdict. Explanations arrive here, never on the read. */
export interface BreakdownEvaluation {
  correct: boolean
  answered: number
  total: number
  prompts: {
    key: string
    prompt: string
    chosenIndex: number
    correct: boolean
    explanation: string
  }[]
}
