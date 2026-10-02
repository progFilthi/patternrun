import { Check } from "lucide-react"

import { isReachable as phaseIsReachable } from "@/lib/training/phases"

/**
 * Stepper for the seven training phases (README section 60).
 *
 * Every phase the learner has already reached is a link they can click back to, so moving
 * between Read, Identify, Animate, Hint and Explain never means restarting the problem. Phases
 * they have not reached yet render as locked, which keeps the one-thing-at-a-time order: the
 * only way forward is still the Continue button, and that button still carries its gate.
 *
 * It sticks under the header, so the way back is always on screen no matter how far down the
 * current phase has scrolled.
 */
export function SessionProgress({
  phases,
  currentIndex,
  reachableIndex,
  onSelect,
}: {
  phases: ReadonlyArray<{ id: string; label: string }>
  currentIndex: number
  /** Highest phase the learner has unlocked. */
  reachableIndex: number
  onSelect: (index: number) => void
}) {
  return (
    <nav
      aria-label="Training phases"
      className="sticky top-16 z-30 -mx-6 overflow-x-auto border-b border-border/70 bg-background/90 px-6 backdrop-blur-md [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
    >
      <ol className="flex w-max items-center gap-1 py-2.5 text-xs">
        {phases.map((phase, index) => {
          const isCurrent = index === currentIndex
          const isReachable = phaseIsReachable(index, reachableIndex)
          const isDone = index < currentIndex

          return (
            <li key={phase.id} className="flex shrink-0 items-center gap-1">
              {index > 0 && <span aria-hidden className="h-px w-3 shrink-0 bg-border sm:w-4" />}
              <button
                type="button"
                onClick={() => onSelect(index)}
                disabled={!isReachable}
                aria-current={isCurrent ? "step" : undefined}
                title={isReachable ? phase.label : `${phase.label} (locked)`}
                className={`group flex shrink-0 items-center gap-1.5 rounded-md px-1.5 py-1 transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none ${
                  isReachable ? "cursor-pointer" : "cursor-not-allowed"
                } ${
                  isCurrent
                    ? "text-foreground"
                    : isReachable
                      ? "text-muted-foreground hover:bg-muted hover:text-foreground"
                      : "text-muted-foreground/45"
                }`}
              >
                <span
                  aria-hidden
                  className={`flex size-5 shrink-0 items-center justify-center rounded-full font-mono text-[10px] transition-colors ${
                    isCurrent
                      ? "bg-foreground font-medium text-background"
                      : isDone
                        ? "bg-muted text-muted-foreground group-hover:text-foreground"
                        : "border border-dashed border-border text-muted-foreground/60"
                  }`}
                >
                  {isDone ? <Check className="size-3" /> : String(index + 1).padStart(2, "0")}
                </span>
                <span className={isCurrent ? "font-medium" : undefined}>
                  <span className="sr-only">
                    {isReachable ? `Go to step ${index + 1}, ${phase.label}` : `Locked: ${phase.label}`}
                  </span>
                  <span aria-hidden>{phase.label}</span>
                </span>
              </button>
            </li>
          )
        })}
      </ol>
    </nav>
  )
}