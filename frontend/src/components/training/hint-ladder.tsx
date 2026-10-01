"use client"

import type { Hint } from "@/types/api"
import { Button } from "@/components/ui/button"

const LEVEL_LABELS: Record<number, string> = {
  1: "Direction",
  2: "Pattern",
  3: "Structure",
  4: "Pseudocode",
  5: "Implementation",
}

/**
 * HINTS: one new idea per rung, revealed only on request (README section 7). The count of
 * revealed rungs is the hint dependency signal, kept for later scoring.
 */
export function HintLadder({
  hints,
  highestRevealed,
  onReveal,
  onContinue,
}: {
  hints: Hint[]
  highestRevealed: number
  onReveal: (level: number) => void
  onContinue: () => void
}) {
  const nextHint = hints.find((hint) => hint.level === highestRevealed + 1)

  return (
    <section aria-label="Step 4: hint ladder" className="flex flex-col gap-8">
      <header className="flex flex-col gap-3 border-b pb-6">
        <p className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          step 4 · hint ladder
        </p>
        <h1 className="text-2xl font-medium tracking-tight">Stuck? Take one rung at a time.</h1>
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
          Each rung adds exactly one idea. Try the next rung only when the current one stops you.
        </p>
      </header>

      <ol className="flex flex-col gap-3">
        {hints.map((hint) => {
          const revealed = hint.level <= highestRevealed
          return (
            <li
              key={hint.level}
              className="rounded-md border border-border px-4 py-3 transition-colors"
              data-state={revealed ? "revealed" : "locked"}
            >
              <div className="flex items-center justify-between gap-4">
                <span className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
                  hint {hint.level} · {LEVEL_LABELS[hint.level] ?? "hint"}
                </span>
                {revealed && hint.level === highestRevealed && (
                  <span className="font-mono text-[11px] text-muted-foreground">highest used</span>
                )}
              </div>
              {revealed ? (
                <p className="mt-2 whitespace-pre-wrap font-mono text-[13px] leading-relaxed">
                  {hint.content}
                </p>
              ) : (
                <p className="mt-2 text-sm text-muted-foreground">Not revealed yet.</p>
              )}
            </li>
          )
        })}
      </ol>

      <div className="flex items-center justify-between gap-4 border-t pt-6">
        <p className="text-sm text-muted-foreground">
          Hints used: {highestRevealed} / {hints.length}
        </p>
        <div className="flex items-center gap-2">
          {nextHint && (
            <Button
              variant="outline"
              onClick={() => onReveal(nextHint.level)}
            >
              Reveal hint {nextHint.level}
            </Button>
          )}
          <Button onClick={onContinue} disabled={highestRevealed === 0}>
            Continue to the explanation
          </Button>
        </div>
      </div>
    </section>
  )
}