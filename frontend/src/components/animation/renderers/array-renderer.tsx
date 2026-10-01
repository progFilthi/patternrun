import { readCellValues, readIndexes, readPointers } from "@/components/animation/payload"
import { Caption, Row, Stage, StageLabel, ValueCell } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** ARRAY: a row of values with optional highlights and named pointer markers. */
export function ArrayRenderer({ payload }: RendererProps) {
  const values = readCellValues(payload, "values")
  const highlights = new Set(readIndexes(payload, "highlight"))
  const pointers = readPointers(payload)
  const pointerByIndex = new Map(pointers.map((pointer) => [pointer.index, pointer.name]))

  return (
    <Stage label="Array visualisation">
      <div>
        <StageLabel>values</StageLabel>
        <Row>
          {values.map((value, index) => (
            <div key={index} className="flex flex-col items-center gap-1">
              <div className="h-4">
                {pointerByIndex.has(index) && (
                  <span className="rounded bg-foreground px-1.5 py-0.5 font-mono text-[11px] text-background">
                    {pointerByIndex.get(index)}
                  </span>
                )}
              </div>
              <ValueCell tone={highlights.has(index) ? "highlight" : "default"}>{value}</ValueCell>
            </div>
          ))}
        </Row>
        {values.length === 0 && <Caption>no values in this step</Caption>}
      </div>
    </Stage>
  )
}