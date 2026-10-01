import { readNumber, readStringArray } from "@/components/animation/payload"
import { Caption, StackColumn, Stage, StageLabel, ValueCell } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** STACK: items waiting for an answer, top of the stack last in the payload. */
export function StackRenderer({ payload }: RendererProps) {
  const items = readStringArray(payload, "items")
  const top = readNumber(payload, "top", items.length - 1)

  return (
    <Stage label="Stack visualisation">
      <div>
        <StageLabel>stack</StageLabel>
        <StackColumn>
          {items.map((item, index) => (
            <ValueCell key={`${item}-${index}`} tone={index === top ? "pointer" : "default"}>
              {item}
            </ValueCell>
          ))}
        </StackColumn>
        <Caption>
          {items.length === 0
            ? "empty"
            : `size = ${items.length}  ·  top = ${top >= 0 ? items[top] : "none"}`}
        </Caption>
      </div>
    </Stage>
  )
}