"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { AlertTriangle, Check, Lightbulb, Loader2, Play, Send, X } from "lucide-react"

import { CodeEditor } from "@/components/editor/code-editor"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import type {
  AttemptCodeState,
  ExecutionResult,
  HintSelection,
  RevealedSolution,
} from "@/types/api"

/**
 * The coding stage.
 *
 * One component, and everything it does exists to make the learner act rather than read. There is
 * no score here, no XP, no combo: what this screen communicates is whether the code works, what went
 * wrong, and what to try next. The rewards arrive afterwards, from the backend, on the completion
 * screen that Phase 3 already had.
 *
 * <h3>Run and Submit are different buttons</h3>
 *
 * Run checks against the examples already on screen. Submit is the backend's evaluation set and is
 * the only thing that can accept a solution. They are visually and behaviourally separate because
 * collapsing them would make "I want to try this again" and "I am done" the same click.
 *
 * <h3>The server decides</h3>
 *
 * Every verdict rendered here arrived in {@link ExecutionResult}. This component never decides
 * whether code passed, never computes what a hint should be, and never asks for one on the
 * learner's behalf. Asking for a hint is a deliberate click, because the fact that someone needed
 * help is the thing the server records.
 */

export interface CodePanelProps {
  attemptId?: string
  /** The entrypoint the backend will call. Shown so the signature is not a guess. */
  entrypoint?: string
  /** Autosave. Called on a pause in typing; failures are the caller's problem, not shown here. */
  onSave: (language: string, code: string) => void
  /** Runs against the visible examples. */
  onRun: (language: string, code: string) => Promise<ExecutionResult>
  /** Evaluates against the hidden set. */
  onSubmit: (language: string, code: string) => Promise<ExecutionResult>
  /** Asks the server which hint fits right now. The stage is sent; the trigger is not. */
  onHint: (stage: "CODING") => Promise<HintSelection | null>
  /** The last escape hatch. Recorded server-side as taken. */
  onReveal: () => Promise<RevealedSolution>
  /** Restores saved source on mount. */
  loadState: (attemptId: string) => Promise<AttemptCodeState>
  /** Records that a rung was read, so hint usage is attributed. */
  onHintRead: (level: number) => void
  /** How long to wait after the last keystroke before autosaving. */
  autosaveDelayMs?: number
}

const STARTER = `def two_sum(nums, target):
    # Return the indices of the two numbers that add up to target.
    ...
`

export function CodePanel({
  attemptId,
  entrypoint,
  onSave,
  onRun,
  onSubmit,
  onHint,
  onReveal,
  loadState,
  onHintRead,
  autosaveDelayMs = 1200,
}: CodePanelProps) {
  const [code, setCode] = useState("")
  const [busy, setBusy] = useState<null | "run" | "submit" | "hint" | "reveal">(null)
  const [result, setResult] = useState<ExecutionResult | null>(null)
  const [hint, setHint] = useState<HintSelection | null>(null)
  const [solution, setSolution] = useState<RevealedSolution | null>(null)
  const [restored, setRestored] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)

  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Restore whatever was saved, so a refresh does not cost the learner their work. Failures are
  // silent on purpose: an autosave that cannot restore is not worth interrupting anyone over, and
  // the editor still works.
  useEffect(() => {
    if (!attemptId || restored) return
    let live = true
    loadState(attemptId)
      .then((state) => {
        if (live && state.code) setCode(state.code)
      })
      .catch(() => undefined)
      .finally(() => {
        if (live) setRestored(true)
      })
    return () => {
      live = false
    }
  }, [attemptId, restored, loadState])

  const change = useCallback(
    (next: string) => {
      setCode(next)
      setFailure(null)
      if (!attemptId) return
      if (saveTimer.current) clearTimeout(saveTimer.current)
      saveTimer.current = setTimeout(() => onSave("PYTHON", next), autosaveDelayMs)
    },
    [attemptId, onSave, autosaveDelayMs],
  )

  const act = useCallback(
    async (what: "run" | "submit" | "hint" | "reveal", run: () => Promise<unknown>) => {
      if (busy) return
      setBusy(what)
      setFailure(null)
      try {
        await run()
      } catch (error) {
        setFailure(error instanceof Error ? error.message : "That did not go through.")
      } finally {
        setBusy(null)
      }
    },
    [busy],
  )

  const runIt = () =>
    act("run", async () => {
      const outcome = await onRun("PYTHON", code)
      setResult(outcome)
      // A fresh result invalidates a hint about the previous one.
      setHint(null)
    })

  const submitIt = () =>
    act("submit", async () => {
      const outcome = await onSubmit("PYTHON", code)
      setResult(outcome)
      setHint(null)
    })

  const askForHint = () =>
    act("hint", async () => {
      const next = await onHint("CODING")
      if (next) {
        setHint(next)
        onHintRead(next.level)
      }
    })

  const reveal = () =>
    act("reveal", async () => {
      setSolution(await onReveal())
    })

  return (
    <section className="space-y-6">
      <header className="space-y-1">
        <h2 className="text-lg tracking-tight">Your solution</h2>
        <p className="text-sm text-muted-foreground">
          Write a Python function called{" "}
          <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-[13px]">
            {entrypoint ?? "the function below"}
          </code>
          . You have the pattern and the pseudocode already; this is where you find out whether you
          understood them.
        </p>
      </header>

      <CodeEditor
        language="python"
        label="Your solution"
        value={code}
        onChange={change}
        placeholder={STARTER}
        rows={14}
      />

      <div className="flex flex-wrap items-center gap-2">
        <Button onClick={runIt} disabled={busy !== null} variant="outline">
          {busy === "run" ? (
            <Loader2 className="size-4 animate-spin" aria-hidden />
          ) : (
            <Play className="size-4" aria-hidden />
          )}
          Run
        </Button>
        <Button onClick={submitIt} disabled={busy !== null}>
          {busy === "submit" ? (
            <Loader2 className="size-4 animate-spin" aria-hidden />
          ) : (
            <Send className="size-4" aria-hidden />
          )}
          Submit
        </Button>
        <Button onClick={askForHint} disabled={busy !== null} variant="ghost" size="sm">
          {busy === "hint" ? (
            <Loader2 className="size-3.5 animate-spin" aria-hidden />
          ) : (
            <Lightbulb className="size-3.5" aria-hidden />
          )}
          Hint
        </Button>
      </div>

      <p className="text-xs text-muted-foreground">
        <strong className="font-medium text-foreground">Run</strong> checks the examples you can
        already see. <strong className="font-medium text-foreground">Submit</strong> runs the full
        evaluation set, including cases you cannot see.
      </p>

      {failure ? (
        <p role="alert" className="flex items-start gap-2 text-sm text-warning">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
          {failure}
        </p>
      ) : null}

      {result ? <ResultPanel result={result} /> : null}
      {hint ? <HintPanel hint={hint} onDismiss={() => setHint(null)} /> : null}

      {solution ? (
        <div className="space-y-2">
          <h3 className="text-sm font-medium">Reference solution</h3>
          <p className="text-sm text-muted-foreground">
            This is the way it is usually written. Read it, then try to write it yourself — the second
            attempt is the one that teaches you something.
          </p>
          <CodeEditor
            language="python"
            label="Reference solution, read only"
            value={solution.code}
            onChange={() => undefined}
            readOnly
            rows={Math.min(16, solution.code.split("\n").length + 1)}
          />
        </div>
      ) : (
        <button
          type="button"
          onClick={reveal}
          disabled={busy !== null}
          className="text-xs text-muted-foreground underline underline-offset-4 transition-colors hover:text-foreground disabled:opacity-50"
        >
          Show me the reference solution
        </button>
      )}
    </section>
  )
}

/**
 * What a run or submit produced.
 *
 * <h3>Hidden cases get one line</h3>
 *
 * The count and nothing else. "All hidden cases passed" is the whole useful signal; naming one of
 * them would hand over the evaluation set, and the backend does not send them anyway.
 *
 * <h3>Failures are not one failure</h3>
 *
 * A wrong answer gets its expected and actual values side by side, because there is something to
 * reason about. A syntax or runtime error gets its line and its message, because there is not.
 */
function ResultPanel({ result }: { result: ExecutionResult }) {
  const visible = result.cases.filter((entry) => !entry.hidden)
  const hiddenTotal = result.cases.filter((entry) => entry.hidden).length
  const hiddenPassed = result.cases.filter((entry) => entry.hidden && entry.passed).length
  // Suppressed when nothing was evaluated. "0 of 5 hidden cases passed" after a runner failure
  // reads as five failures, and a learner who reads that believes their code is wrong when it was
  // never run at all.
  const evaluated = result.casesTotal > 0

  return (
    <div className="space-y-3" aria-live="polite">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={result.outcome === "ACCEPTED" ? "default" : "outline"}>
          {outcomeLabel(result)}
        </Badge>
        {evaluated && result.kind === "RUN" && result.outcome === "ACCEPTED" ? (
          // The most important sentence on this screen. A passing Run is not a solve, and a
          // learner who cannot tell that will assume it is.
          <span className="text-sm text-muted-foreground">
            Visible examples pass. Submit to be checked against the full set.
          </span>
        ) : null}
        {evaluated && hiddenTotal > 0 ? (
          <span className="text-sm text-muted-foreground">
            {hiddenPassed} of {hiddenTotal} hidden cases passed
          </span>
        ) : null}
        {evaluated && result.durationMs !== undefined ? (
          <span className="font-mono text-xs text-muted-foreground">{result.durationMs}ms</span>
        ) : null}
      </div>

      {result.error ? (
        <div className="rounded-md border border-destructive/40 bg-destructive/5 p-4">
          <div className="flex items-baseline gap-2">
            <span className="font-mono text-[13px] text-destructive">{result.error.type}</span>
            {result.error.line ? (
              <span className="text-xs text-muted-foreground">line {result.error.line}</span>
            ) : null}
          </div>
          <p className="mt-1 font-mono text-[13px] leading-relaxed">{result.error.message}</p>
        </div>
      ) : null}

      {visible.length > 0 ? (
        <ul className="divide-y rounded-md border">
          {visible.map((entry) => (
            <li key={entry.ordinal} className="flex flex-col gap-1 p-3 text-[13px]">
              <div className="flex items-center gap-2">
                {entry.passed ? (
                  <Check className="size-3.5 text-success" aria-label="passed" />
                ) : (
                  <X className="size-3.5 text-danger" aria-label="failed" />
                )}
                <span className="font-medium">{entry.label}</span>
              </div>
              <p className="font-mono text-muted-foreground">{entry.input}</p>
              {!entry.passed ? (
                <div className="grid gap-1 sm:grid-cols-2">
                  <p className="font-mono">
                    <span className="text-muted-foreground">expected </span>
                    {entry.expected}
                  </p>
                  <p className="font-mono">
                    <span className="text-muted-foreground">you returned </span>
                    {entry.actual ?? "nothing — it raised before returning"}
                  </p>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}

/**
 * One rung.
 *
 * Shown on its own, never as a list. A hint panel that displays three at once is an explanation
 * screen wearing a hint's clothes, and the learner learns to stop reading them.
 */
function HintPanel({ hint, onDismiss }: { hint: HintSelection; onDismiss: () => void }) {
  return (
    <div className="rounded-md border-l-2 border-l-foreground/30 bg-surface p-4">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm leading-relaxed">{hint.content}</p>
        <button
          type="button"
          onClick={onDismiss}
          className="shrink-0 text-muted-foreground transition-colors hover:text-foreground"
          title="Dismiss this hint"
        >
          <X className="size-3.5" aria-hidden />
          <span className="sr-only">Dismiss this hint</span>
        </button>
      </div>
    </div>
  )
}

/**
 * The headline, phrased as what happened rather than as a grade.
 *
 * A wrong answer is not an error and a crash is not a wrong answer, and the wording is the only
 * place that distinction lands before the learner reads the detail.
 */
function outcomeLabel(result: ExecutionResult): string {
  switch (result.outcome) {
    case "ACCEPTED":
      return result.kind === "SUBMIT" ? "Accepted" : "Visible examples pass"
    case "WRONG_ANSWER":
      return "Wrong answer"
    case "SYNTAX_ERROR":
      return "Syntax error"
    case "RUNTIME_ERROR":
      return "Runtime error"
    case "TIME_LIMIT_EXCEEDED":
      return "Too slow"
    case "MEMORY_LIMIT_EXCEEDED":
      return "Out of memory"
    case "INTERNAL_ERROR":
      return "The runner could not evaluate this"
  }
}