"use client"

import type { Complexity } from "@/types/api"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { normalize } from "@/lib/training/complexity"

/** COMPLEXITY: the learner commits before the official answer is shown (README section 11). */
export function ComplexityCheck({
  expected,
  options,
  timeChoice,
  spaceChoice,
  checked,
  correct,
  onTimeChange,
  onSpaceChange,
  onCheck,
  onContinue,
}: {
  expected: Complexity
  options: readonly string[]
  timeChoice: string | null
  spaceChoice: string | null
  checked: boolean
  correct: boolean
  onTimeChange: (value: string) => void
  onSpaceChange: (value: string) => void
  onCheck: () => void
  onContinue: () => void
}) {
  return (
    <section aria-label="Step 6: complexity" className="flex flex-col gap-8">
      <header className="flex flex-col gap-3 border-b pb-6">
        <p className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          step 6 · complexity
        </p>
        <h1 className="text-2xl font-medium tracking-tight">Before you look, what is it?</h1>
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
          Count the operations the loop performs, not the ones you hope it performs.
        </p>
      </header>

      <div className="grid gap-10 md:grid-cols-2">
        <ChoiceGroup
          legend="Time"
          options={options}
          value={timeChoice}
          expected={expected.time}
          checked={checked}
          onChange={onTimeChange}
        />
        <ChoiceGroup
          legend="Space"
          options={options}
          value={spaceChoice}
          expected={expected.space}
          checked={checked}
          onChange={onSpaceChange}
        />
      </div>

      {checked && (
        <p className={cn("text-sm", correct ? "text-success" : "text-danger")}>
          {correct
            ? `Correct: O time and O space. ${
                expected.time === "O(n)" ? "One pass, one element at a time." : ""
              }`
            : `The answer is ${expected.time} time and ${expected.space} space. Count the passes again.`}
        </p>
      )}

      <div className="flex justify-end border-t pt-6">
        {checked ? (
          /* Phase 4 changed where this goes, so the label follows. It used to say "Finish the
             session" and it did: complexity was the last thing before the rewards. Now it leads to
             the editor, and a button that promises to finish the session while opening a blank
             editor is the kind of small lie that makes an interface feel unreliable. */
          <Button onClick={onContinue}>Write it</Button>
        ) : (
          <Button onClick={onCheck} disabled={!timeChoice || !spaceChoice}>
            Check my complexity
          </Button>
        )}
      </div>
    </section>
  )
}

function ChoiceGroup({
  legend,
  options,
  value,
  expected,
  checked,
  onChange,
}: {
  legend: string
  options: readonly string[]
  value: string | null
  expected: string
  checked: boolean
  onChange: (value: string) => void
}) {
  return (
    <fieldset>
      <legend className="mb-3 text-xs font-medium uppercase tracking-[0.08em] text-muted-foreground">
        {legend}
      </legend>
      <div role="radiogroup" aria-label={legend} className="flex flex-wrap gap-2">
        {options.map((option) => {
          const selected = value === option
          const isExpected = normalize(option) === normalize(expected)
          return (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={selected}
              disabled={checked}
              onClick={() => onChange(option)}
              className={cn(
                "rounded-md border px-3 py-2 font-mono text-sm transition-colors disabled:cursor-default",
                checked && isExpected && "border-success",
                checked && selected && !isExpected && "border-danger",
                !checked && (selected ? "border-foreground" : "border-border hover:border-foreground/40"),
              )}
            >
              {option}
            </button>
          )
        })}
      </div>
    </fieldset>
  )
}