import { readBoolean, readCellValues, readNumber } from "@/components/animation/payload"
import { Caption, Row, Stage, StageLabel, ValueCell } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** WINDOW: the contiguous region currently under observation. */
export function WindowRenderer({ payload }: RendererProps) {
  const values = readCellValues(payload, "values")
  const left = readNumber(payload, "left", 0)
  const right = readNumber(payload, "right", -1)
  const valid = readBoolean(payload, "valid", true)
  const empty = right < left

  return (
    <Stage label="Sliding window visualisation">
      <div>
        <StageLabel>window</StageLabel>
        <Row>
          {values.map((value, index) => {
            const inWindow = !empty && index >= left && index <= right
            return (
              <div key={index} className="flex flex-col items-center gap-1">
                <div className="h-4 font-mono text-[11px] text-muted-foreground">
                  {index === left && !empty ? "L" : null}
                </div>
                <ValueCell tone={inWindow ? "window" : "muted"}>{value}</ValueCell>
                <div className="h-4 font-mono text-[11px] text-muted-foreground">
                  {index === right && !empty ? "R" : null}
                </div>
              </div>
            )
          })}
        </Row>
        <Caption>
          {empty
            ? "window is empty"
            : `window = [${left}, ${right}]  ·  length = ${right - left + 1}  ·  ${
                valid ? "valid" : "invalid"
              }`}
        </Caption>
      </div>
    </Stage>
  )
}