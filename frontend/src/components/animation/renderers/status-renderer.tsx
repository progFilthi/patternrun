import { readString } from "@/components/animation/payload"
import { Stage } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** SUCCESS and FAILURE: the end of a walkthrough, and the lesson when it goes wrong. */
export function StatusRenderer({ payload, tone }: RendererProps & { tone: "success" | "failure" }) {
  const message = readString(payload, "message")
  const lesson = readString(payload, "lesson")

  return (
    <Stage label={tone === "success" ? "Result" : "Failure"}>
      <div className="max-w-lg text-center">
        <p
          className={`font-mono text-xs uppercase tracking-[0.12em] ${
            tone === "success" ? "text-success" : "text-danger"
          }`}
        >
          {tone === "success" ? "done" : "not this time"}
        </p>
        <p className="mt-2 text-[15px] leading-relaxed">{message}</p>
        {lesson && <p className="mt-4 border-l-2 border-border pl-3 text-left text-sm text-muted-foreground">{lesson}</p>}
      </div>
    </Stage>
  )
}