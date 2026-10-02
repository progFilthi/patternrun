"use client"

import { useEffect, useState } from "react"
import { Check, Loader2, X } from "lucide-react"

import { fetchBreakdown } from "@/lib/api/session"
import type { BreakdownEvaluation, BreakdownPrompt } from "@/types/api"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/**
 * The reading step: work out what the problem is actually asking before reaching for a tool.
 *
 * This exists because the hard part of a problem is rarely the algorithm. It is working out what
 * you have been given, what you owe back, and which constraint actually narrows the field. A
 * learner who has not done that cannot recognise the signal however well they know the tools, so
 * this sits in the Read phase, before the pattern choice rather than after it.
 *
 * The prompts arrive without their answers. That is deliberate and not an oversight: the read
 * endpoint projects away the key, so the verdict has to come back from the server, and the
 * explanations are shown only after a choice is committed.
 */
/**
 * The closing judgement, kept apart so it can render whether or not the options are on screen.
 */
function Verdict({ submitted }: { submitted: BreakdownEvaluation }) {
  return (
    <p className={cn("text-sm", submitted.correct ? "text-success" : "text-muted-foreground")}>
      {submitted.correct
        ? "You read it correctly. That is the hard part."
        : "Worth another look at the statement before you reach for a pattern."}
    </p>
  )
}

export function ProblemBreakdown({
  slug,
  submitted,
  pending,
  onSubmit,
}: {
  slug: string
  submitted?: BreakdownEvaluation
  pending: boolean
  onSubmit: (answers: { key: string; chosenIndex: number }[]) => void
}) {
  const [prompts, setPrompts] = useState<BreakdownPrompt[] | null>(null)
  const [unavailable, setUnavailable] = useState(false)
  const [choices, setChoices] = useState<Record<string, number>>({})
  const [expanded, setExpanded] = useState(false)

  useEffect(() => {
    let live = true
    fetchBreakdown(slug)
      .then((loaded) => {
        if (live) setPrompts(loaded)
      })
      .catch(() => {
        // Not every problem is written yet. A missing breakdown is a normal state, not an error,
        // and the learner should still be able to read the problem and train.
        if (live) setUnavailable(true)
      })
    return () => {
      live = false
    }
  }, [slug])

  if (unavailable || (prompts !== null && prompts.length === 0)) {
    return null
  }

  // A verdict can outlive the prompt list, so it is rendered on its own terms. Anything else
  // would leave a learner staring at a skeleton while the explanation that is already in hand
  // waited on a request that no longer matters.
  const verdictByKey = new Map(submitted?.prompts.map((entry) => [entry.key, entry]))

  if (prompts === null) {
    return submitted ? (
      <section aria-label="Read the problem" className="flex flex-col gap-4 border-t pt-6">
        <h2 className="text-sm font-medium tracking-tight">Before you pick a pattern</h2>
        <Verdict submitted={submitted} />
      </section>
    ) : (
      <section aria-label="Read the problem" className="flex flex-col gap-3">
        <div className="h-4 w-32 animate-pulse rounded bg-muted" />
      </section>
    )
  }

  const answeredAll = prompts.every((prompt) => choices[prompt.key] !== undefined)

  return (
    <section aria-label="Read the problem" className="flex flex-col gap-4 border-t pt-6">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2">
        <div className="flex flex-col gap-1">
          <h2 className="text-sm font-medium tracking-tight">Before you pick a pattern</h2>
          <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground">
            Three questions about what this problem is asking. Reading it correctly is most of
            the work.
          </p>
        </div>
        {!submitted && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setExpanded((current) => !current)}
            aria-expanded={expanded}
          >
            {expanded ? "Hide" : "Show"}
          </Button>
        )}
      </div>

      {(expanded || submitted) && (
        <ol className="flex flex-col gap-5">
          {prompts.map((prompt) => {
            const verdict = verdictByKey.get(prompt.key)
            return (
              <li key={prompt.key} className="flex flex-col gap-2">
                <p className="text-[15px] font-medium">{prompt.prompt}</p>
                <div role="radiogroup" aria-label={prompt.prompt} className="flex flex-col gap-1.5">
                  {prompt.options.map((option, index) => {
                    const chosen = choices[prompt.key] === index
                    const isRight = verdict?.correct && verdict.chosenIndex === index
                    const isWrong = verdict && !verdict.correct && verdict.chosenIndex === index

                    return (
                      <button
                        key={option}
                        type="button"
                        role="radio"
                        aria-checked={chosen || verdict?.chosenIndex === index}
                        // Locked once answered: changing the answer after seeing the explanation
                        // would be revising the record rather than reading it.
                        disabled={Boolean(verdict) || pending}
                        onClick={() =>
                          setChoices((current) => ({ ...current, [prompt.key]: index }))
                        }
                        className={cn(
                          "flex items-center gap-3 rounded-md border px-3 py-2 text-left text-sm transition-colors",
                          "disabled:cursor-default",
                          isRight && "border-success bg-success/5",
                          isWrong && "border-danger bg-danger/5",
                          !isRight && !isWrong && chosen && "border-foreground",
                          !isRight && !isWrong && !chosen && !verdict && "border-border hover:border-foreground/40 hover:bg-secondary",
                          verdict && !chosen && !isWrong && "border-border opacity-60",
                        )}
                      >
                        <span aria-hidden className="font-mono text-xs text-muted-foreground">
                          {String.fromCharCode(65 + index)}
                        </span>
                        <span className="flex-1">{option}</span>
                        {isRight && (
                          <span aria-label="correct" className="text-success">
                            <Check className="size-3.5" />
                          </span>
                        )}
                        {isWrong && (
                          <span aria-label="not what the problem asked for" className="text-danger">
                            <X className="size-3.5" />
                          </span>
                        )}
                      </button>
                    )
                  })}
                </div>
                {verdict && (
                  <p className="max-w-2xl border-l-2 border-border pl-3 text-sm leading-relaxed text-muted-foreground">
                    {verdict.explanation}
                  </p>
                )}
              </li>
            )
          })}
        </ol>
      )}

      {!submitted && (
        <div className="flex items-center justify-between gap-4">
          <p className="text-xs text-muted-foreground">
            {answeredAll
              ? "All three answered."
              : `${Object.keys(choices).length} of ${prompts.length} answered.`}
          </p>
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              disabled={!answeredAll || pending}
              onClick={() =>
                onSubmit(
                  prompts.map((prompt) => ({
                    key: prompt.key,
                    chosenIndex: choices[prompt.key],
                  })),
                )
              }
            >
              {pending && <Loader2 aria-hidden className="size-3.5 animate-spin" />}
              {pending ? "Checking…" : "Check my reading"}
            </Button>
          </div>
        </div>
      )}

      {submitted && <Verdict submitted={submitted} />}
    </section>
  )
}