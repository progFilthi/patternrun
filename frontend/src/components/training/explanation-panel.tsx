import { AlertTriangle, Ban, Gauge, Quote } from "lucide-react"

import type { ProblemDetail } from "@/types/api"

/**
 * PSEUDOCODE plus the reasoning that makes the solution stick.
 *
 * The four fields this renders are all specific, but set as four same-weight paragraphs they
 * read as a wall and feel vague. So they are ordered as an argument instead: the invariant is
 * the claim, the pseudocode is the machine that maintains it, the two explanations are why it
 * is correct and what it beat, and the mistakes are what to avoid. Giving the invariant the
 * weight it deserves is what makes the step legible.
 */
export function ExplanationPanel({ problem }: { problem: ProblemDetail }) {
  return (
    <div className="flex flex-col gap-12">
      <header className="flex flex-col gap-3 border-b pb-6">
        <p className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          step 5 · explain
        </p>
        <h1 className="text-2xl font-medium tracking-tight">Write it down, then say it out loud.</h1>
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
          A solution is one sentence you can defend. Find that sentence first: state what stays
          true, show the code that keeps it true, then say what you rejected and why.
        </p>
      </header>

      <section aria-label="The invariant" className="flex flex-col gap-3">
        <h2 className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          <Gauge aria-hidden className="size-3.5" />
          the invariant
        </h2>
        <div className="rounded-lg border border-l-2 border-l-foreground/30 bg-surface px-6 py-6">
          <p className="max-w-3xl text-lg leading-relaxed tracking-tight text-balance">
            {problem.invariant}
          </p>
          <p className="mt-4 max-w-3xl text-sm leading-relaxed text-muted-foreground">
            This sentence is true after every step of the loop. If it holds to the end, the
            answer you are holding is correct, which is why the rest of the explanation is just
            a consequence of it.
          </p>
        </div>
      </section>

      <section aria-label="Pseudocode" className="flex flex-col gap-3">
        <h2 className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          the code that keeps it true
        </h2>
        <pre className="overflow-x-auto rounded-lg border bg-code p-5 font-mono text-[13px] leading-relaxed">
          <code>{problem.pseudocode.join("\n")}</code>
        </pre>
      </section>

      <section aria-label="Why it works and why not brute force" className="grid gap-10 md:grid-cols-2">
        <div className="flex flex-col gap-3">
          <h2 className="font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
            why it works
          </h2>
          <p className="text-[15px] leading-relaxed">{problem.interviewExplanation}</p>
        </div>
        <div className="flex flex-col gap-3">
          <h2 className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
            <Ban aria-hidden className="size-3.5" />
            what it beats
          </h2>
          <p className="text-[15px] leading-relaxed text-muted-foreground">{problem.bruteForce}</p>
        </div>
      </section>

      <section aria-label="How to say it" className="flex flex-col gap-4 rounded-lg border bg-surface px-6 py-6">
        <h2 className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          <Quote aria-hidden className="size-3.5" />
          say it in one sentence
        </h2>
        <p className="max-w-3xl text-[15px] leading-relaxed">
          Answer these three out loud, in order. If you can, you know the solution; if you
          cannot, you know exactly which part to go back to.
        </p>
        <ol className="flex max-w-3xl flex-col gap-2.5">
          {SAY_IT_BEATS.map((beat, index) => (
            <li key={beat} className="flex gap-3 text-[15px] leading-relaxed">
              <span
                aria-hidden
                className="flex size-5 shrink-0 items-center justify-center rounded-full border font-mono text-[10px] text-muted-foreground"
              >
                {index + 1}
              </span>
              <span>{beat.replace("{pattern}", problem.pattern.name)}</span>
            </li>
          ))}
        </ol>
      </section>

      <section aria-label="Common mistakes" className="flex flex-col gap-4 border-t pt-8">
        <h2 className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground">
          <AlertTriangle aria-hidden className="size-3.5" />
          mistakes to avoid
        </h2>
        <ul className="flex max-w-3xl flex-col gap-2.5">
          {problem.commonMistakes.map((mistake) => (
            <li key={mistake} className="flex gap-3 text-[15px] leading-relaxed">
              <span aria-hidden className="text-danger">
                ×
              </span>
              <span>{mistake}</span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}

/** The three beats of a defensable one-sentence answer, in the order they must be said. */
const SAY_IT_BEATS = [
  "Name the signal in the problem that pointed at {pattern}.",
  "State the invariant, and what the code does to maintain it.",
  "Give the complexity, and the alternative you rejected to get there.",
]