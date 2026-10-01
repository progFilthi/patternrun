import { readCellValues, readNumber, readNumberArray } from "@/components/animation/payload"
import { Caption, Row, Stage, StageLabel, ValueCell } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** PREFIX_SUM: the input row above the running prefix totals, aligned by index. */
export function PrefixSumRenderer({ payload }: RendererProps) {
  const values = readCellValues(payload, "values")
  const prefix = readNumberArray(payload, "prefix")
  const currentIndex = readNumber(payload, "currentIndex", 0)

  return (
    <Stage label="Prefix sum visualisation">
      <div>
        <StageLabel>prefix</StageLabel>
        <Row>
          {values.map((value, index) => (
            <div key={index} className="flex flex-col items-center gap-1">
              <ValueCell tone="default">{value}</ValueCell>
              <span className="text-muted-foreground/60">↓</span>
              <ValueCell tone={index === currentIndex ? "highlight" : "muted"}>
                {prefix[index + 1] ?? "?"}
              </ValueCell>
            </div>
          ))}
        </Row>
        <Caption>
          prefix[0] = {prefix[0] ?? 0} · current index = {currentIndex}
        </Caption>
      </div>
    </Stage>
  )
}