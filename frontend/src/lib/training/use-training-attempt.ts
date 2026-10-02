"use client"

import { useCallback, useRef, useState } from "react"

import {
  SessionApiError,
  completeAttempt,
  fetchCodeState,
  fetchNextHint,
  recordHint as sendHint,
  recordPattern as sendPattern,
  recordPrediction as sendPrediction,
  revealSolution,
  runCode,
  saveCode,
  startAttempt as sendStart,
  submitBreakdown,
  submitCode,
} from "@/lib/api/session"
import type {
  AttemptCodeState,
  AttemptMode,
  BreakdownEvaluation,
  CompletionResult,
  ExecutionResult,
  HintSelection,
  HintStage,
  RevealedSolution,
} from "@/types/api"

/**
 * Drives one training session against the backend.
 *
 * The rules this encodes are the reason the file exists.
 *
 * <p><b>The server decides.</b> Nothing here computes XP, a combo, a grade, or whether an answer
 * was right. Each call sends the learner's choice and returns the server's answer. The final
 * display is {@link CompletionResult}, returned verbatim.
 *
 * <p><b>A failure is shown, never papered over.</b> While a completion is in flight, and if it
 * fails, the UI says so and offers a retry. It does not invent a result, and it does not write a
 * fabricated completion to local storage, because a made-up total is worse than an honest gap.
 *
 * <p><b>Repeating a completion is safe.</b> Completion is guarded against concurrent sends, and
 * the backend treats a retry as a replay, so recovering from a timeout does not double-credit.
 */

export type SessionPhase =
  | "idle"
  | "starting"
  | "active"
  | "submitting-action"
  | "completing"
  | "complete"
  | "failed"

/**
 * Phase 4 adds one: the coding stage is in flight.
 *
 * Separate from {@link SessionPhase} because it is not a state of the session but a state of one
 * request. A learner running code has not left the session and has not finished it, and folding
 * this into the phase would make `phase === "active"` mean something different depending on which
 * phase of the loop was on screen.
 */
export type CodePhase = "idle" | "saving" | "running" | "submitting" | "hinting" | "revealing"

export interface CompletionInput {
  complexityTime: string
  complexitySpace: string
  durationMs: number
}

export interface SessionState {
  phase: SessionPhase
  /** The server's attempt id, once the session exists. */
  attemptId?: string
  /** The learner's choices, echoed back so the UI can show what it submitted. */
  patternChoice?: string
  hintsUsed: number
  predictions: { stepOrder: number; chosenIndex: number }[]
  /** The backend's verdict. Absent until a completion has actually succeeded. */
  result?: CompletionResult
  /** Why the last call failed, in words a learner can act on. */
  error?: string
  /** Whether retrying the same call could plausibly work. */
  retryable?: boolean
  /** Kept so a failed completion can be re-sent verbatim rather than reconstructed. */
  pendingCompletion?: CompletionInput
  /** The server's verdict on how the problem was read. Absent until the step is submitted. */
  breakdown?: BreakdownEvaluation
  breakdownPending: boolean
  /** Phase 4: what one run or submit concluded. The server's, never computed here. */
  code?: ExecutionResult
  /** Phase 4: which coding request is in flight, if any. */
  codePhase: CodePhase
}

const INITIAL: SessionState = {
  phase: "idle",
  hintsUsed: 0,
  predictions: [],
  breakdownPending: false,
  codePhase: "idle",
}

export function useTrainingAttempt(problemSlug: string, mode: AttemptMode = "STANDARD") {
  const [state, setState] = useState<SessionState>(INITIAL)
  // A ref, not state: two clicks in the same tick must see the same guard.
  const inFlight = useRef(false)

  const fail = useCallback((error: unknown) => {
    const apiError =
      error instanceof SessionApiError
        ? error
        : new SessionApiError("server", 0, "Something went wrong. Try again.")

    setState((current) => ({
      ...current,
      phase: "failed",
      // A lost session is recoverable by the next call, so it reads as retryable rather than
      // as a dead end. Telling someone their session died when one retry would fix it is the
      // fastest way to make them stop.
      error: apiError.message,
      retryable: apiError.isRetryable || apiError.kind === "session",
    }))
  }, [])

  /** Opens the session. Safe to call twice: the server resumes rather than forking. */
  const start = useCallback(async () => {
    if (inFlight.current) return
    inFlight.current = true
    setState((current) => ({ ...current, phase: "starting", error: undefined }))

    try {
      const attempt = await sendStart(problemSlug, mode)
      setState((current) => ({
        ...current,
        phase: "active",
        attemptId: attempt.id,
        hintsUsed: attempt.hintsUsed,
        predictions: current.predictions,
      }))
    } catch (error) {
      fail(error)
    } finally {
      inFlight.current = false
    }
  }, [problemSlug, mode, fail])

  /** Runs an action against the live attempt, showing that an action is in flight. */
  const withAction = useCallback(
    async (action: (attemptId: string) => Promise<unknown>) => {
      const attemptId = state.attemptId
      if (!attemptId || inFlight.current) return
      inFlight.current = true
      setState((current) => ({ ...current, phase: "submitting-action", error: undefined }))

      try {
        await action(attemptId)
        setState((current) => ({ ...current, phase: "active" }))
      } catch (error) {
        fail(error)
      } finally {
        inFlight.current = false
      }
    },
    [state.attemptId, fail],
  )

  const choosePattern = useCallback(
    (patternSlug: string) => {
      // Recorded locally immediately so the grid reflects the choice at once. The backend is
      // still told what was picked, and decides whether it was right.
      setState((current) => ({ ...current, patternChoice: patternSlug }))
      return withAction((attemptId) => sendPattern(attemptId, patternSlug))
    },
    [withAction],
  )

  const revealHint = useCallback(
    (level: number) => {
      setState((current) => ({ ...current, hintsUsed: Math.max(current.hintsUsed, level) }))
      return withAction((attemptId) => sendHint(attemptId, level))
    },
    [withAction],
  )

  const predict = useCallback(
    (stepOrder: number, chosenIndex: number) => {
      setState((current) => ({
        ...current,
        predictions: [
          ...current.predictions.filter((entry) => entry.stepOrder !== stepOrder),
          { stepOrder, chosenIndex },
        ],
      }))
      return withAction((attemptId) => sendPrediction(attemptId, stepOrder, chosenIndex))
    },
    [withAction],
  )

  /**
   * Submits the breakdown answers and keeps the server's verdict.
   *
   * Whether the problem was read correctly is decided when these answers arrive, not later.
   * Nothing about that verdict is sent back to the server, because the server already worked it
   * out and stored it against the attempt.
   */
  const submitReading = useCallback(
    async (answers: { key: string; chosenIndex: number }[]) => {
      if (inFlight.current || !state.attemptId) return
      inFlight.current = true
      setState((current) => ({ ...current, breakdownPending: true, error: undefined }))

      try {
        const evaluation = await submitBreakdown(state.attemptId, answers)
        setState((current) => ({
          ...current,
          breakdownPending: false,
          breakdown: evaluation,
          phase: current.phase === "idle" ? "active" : current.phase,
        }))
      } catch (error) {
        inFlight.current = false
        fail(error)
        return
      }
      inFlight.current = false
    },
    [state.attemptId, fail],
  )

  /**
   * The saved source and the last verdict, so a reload can restore what was being written.
   *
   * A read that decides nothing. Rejecting when there is no attempt yet is deliberate: there is
   * nothing to restore, and reporting that as a failure would make the editor look broken.
   */
  const loadCode = useCallback(async (): Promise<AttemptCodeState | null> => {
    if (!state.attemptId) return null
    return fetchCodeState(state.attemptId)
  }, [state.attemptId])

  /**
   * Autosave.
   *
   * Fires after a pause in typing, so it shares no guard with the other actions: it must not
   * cancel a submission, and a submission must not cancel it. Failures are swallowed, because a
   * save that did not land is worth nothing compared to interrupting someone mid-sentence, and the
   * next pause tries again.
   */
  const persistCode = useCallback(
    (language: string, code: string) => {
      if (!state.attemptId) return
      setState((current) => ({ ...current, codePhase: "saving" }))
      void saveCode(state.attemptId, language, code)
        .catch(() => undefined)
        .finally(() => setState((current) => ({ ...current, codePhase: "idle" })))
    },
    [state.attemptId],
  )

  /**
   * Runs code against the visible examples and returns the server's verdict verbatim.
   *
   * Throws on a transport failure so the panel can say the request did not arrive, which is a
   * different situation from a request that arrived and came back WRONG_ANSWER. Conflating them
   * would tell a learner their code failed when the network did.
   */
  const runCodeFor = useCallback(
    async (language: string, code: string): Promise<ExecutionResult> => {
      if (!state.attemptId) {
        throw new SessionApiError("session", 0, "This session never started. Go back and try again.")
      }
      setState((current) => ({ ...current, codePhase: "running" }))
      try {
        const result = await runCode(state.attemptId, language, code)
        setState((current) => ({ ...current, code: result, codePhase: "idle" }))
        return result
      } catch (error) {
        setState((current) => ({ ...current, codePhase: "idle" }))
        throw error
      }
    },
    [state.attemptId],
  )

  /**
   * Evaluates against the hidden set.
   *
   * The only request that can make a problem solved, which is exactly why it is the only one that
   * takes this path. Nothing it returns is interpreted here; the backend's outcome is displayed.
   */
  const submitCodeFor = useCallback(
    async (language: string, code: string): Promise<ExecutionResult> => {
      if (!state.attemptId) {
        throw new SessionApiError("session", 0, "This session never started. Go back and try again.")
      }
      setState((current) => ({ ...current, codePhase: "submitting" }))
      try {
        const result = await submitCode(state.attemptId, language, code)
        setState((current) => ({ ...current, code: result, codePhase: "idle" }))
        return result
      } catch (error) {
        setState((current) => ({ ...current, codePhase: "idle" }))
        throw error
      }
    },
    [state.attemptId],
  )

  /**
   * Asks which hint fits right now.
   *
   * The stage travels and the trigger does not. The backend derives the trigger from the last
   * execution it observed, so a client cannot request the debugging ladder after a passing run and
   * walk to the answer without ever having failed.
   */
  const askForHint = useCallback(
    async (stage: HintStage = "CODING"): Promise<HintSelection | null> => {
      if (!state.attemptId) return null
      setState((current) => ({ ...current, codePhase: "hinting" }))
      try {
        const hint = await fetchNextHint(state.attemptId, stage)
        setState((current) => ({ ...current, codePhase: "idle" }))
        return hint
      } catch (error) {
        setState((current) => ({ ...current, codePhase: "idle" }))
        throw error
      }
    },
    [state.attemptId],
  )

  /**
   * The reference implementation, and the fact that it was taken.
   *
   * Recorded server-side before the code is returned, so there is no path to the answer that does
   * not also mark the session as assisted.
   */
  const revealReference = useCallback(async (): Promise<RevealedSolution> => {
    if (!state.attemptId) {
      throw new SessionApiError("session", 0, "This session never started. Go back and try again.")
    }
    setState((current) => ({ ...current, codePhase: "revealing" }))
    try {
      const revealed = await revealSolution(state.attemptId)
      setState((current) => ({ ...current, codePhase: "idle" }))
      return revealed
    } catch (error) {
      setState((current) => ({ ...current, codePhase: "idle" }))
      throw error
    }
  }, [state.attemptId])

  /**
   * Finishes the session and takes the server's verdict.
   *
   * `breakdownCorrect` is the result of the problem-reading check the learner just did. It is a
   * statement about what they did, not a claim that an answer was correct, so it belongs here.
   * Everything else about whether they were right is decided by the backend.
   */
  const complete = useCallback(
    async (input: CompletionInput): Promise<CompletionResult | undefined> => {
      if (inFlight.current) return undefined
      if (!state.attemptId) {
        setState((current) => ({
          ...current,
          phase: "failed",
          error: "This session never started. Go back and try again.",
          retryable: false,
        }))
        return undefined
      }

      inFlight.current = true
      // Kept verbatim so a retry sends exactly what was attempted, not a reconstruction that
      // could differ if the learner changed an answer in between.
      setState((current) => ({
        ...current,
        phase: "completing",
        error: undefined,
        pendingCompletion: input,
      }))

      try {
        const result = await completeAttempt(state.attemptId, {
          complexityTime: input.complexityTime,
          complexitySpace: input.complexitySpace,
          durationMs: input.durationMs,
          predictions: state.predictions,
        })
        // The response is the whole truth about this session. Nothing is added to it.
        setState((current) => ({ ...current, phase: "complete", result }))
        return result
      } catch (error) {
        fail(error)
        return undefined
      } finally {
        inFlight.current = false
      }
    },
    [state.attemptId, state.predictions, fail],
  )

  /**
   * Re-sends the completion that failed.
   *
   * Sends the stored payload rather than nothing, because the backend treats a repeat completion
   * as a replay of the recorded result. A learner who lost their connection at the worst moment
   * should not have to redo the session to find out what it was worth.
   */
  const retry = useCallback((): Promise<CompletionResult | undefined> => {
    const pending = state.pendingCompletion
    if (!pending) {
      setState((current) => ({ ...current, phase: "active", error: undefined }))
      return Promise.resolve(undefined)
    }
    inFlight.current = false
    return complete(pending)
  }, [state.pendingCompletion, complete])

  return {
    state,
    start,
    choosePattern,
    revealHint,
    predict,
    submitReading,
    loadCode,
    persistCode,
    runCode: runCodeFor,
    submitCode: submitCodeFor,
    askForHint,
    revealReference,
    complete,
    retry,
  }
}

export type TrainingAttempt = ReturnType<typeof useTrainingAttempt>