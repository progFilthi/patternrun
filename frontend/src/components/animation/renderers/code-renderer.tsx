import { readString } from "@/components/animation/payload"
import { Stage, StageLabel } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** CODE: a snippet shown as part of the explanation. */
export function CodeRenderer({ payload }: RendererProps) {
  const code = readString(payload, "code")
  const language = readString(payload, "language", "text")

  return (
    <Stage label="Code visualisation">
      <div className="w-full self-stretch text-left">
        <StageLabel>{language}</StageLabel>
        <pre className="max-h-72 overflow-auto rounded border bg-code p-4 font-mono text-[13px] leading-relaxed">
          <code>{code}</code>
        </pre>
      </div>
    </Stage>
  )
}