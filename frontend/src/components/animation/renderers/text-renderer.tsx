import { readString, readStringArray } from "@/components/animation/payload"
import { Stage, StageLabel } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** TEXT: the conceptual steps, such as "what are we asked to find". */
export function TextRenderer({ payload }: RendererProps) {
  const lines = readStringArray(payload, "lines")
  const single = readString(payload, "text")

  return (
    <Stage label="Text step">
      <div className="self-stretch text-left">
        <StageLabel>note</StageLabel>
        {lines.length > 0 ? (
          <ul className="space-y-1 font-mono text-sm leading-relaxed">
            {lines.map((line, index) => (
              <li key={index}>{line}</li>
            ))}
          </ul>
        ) : (
          <p className="text-sm">{single}</p>
        )}
      </div>
    </Stage>
  )
}