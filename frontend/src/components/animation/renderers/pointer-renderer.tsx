import { readCellValues, readIndexes, readPointers } from "@/components/animation/payload"
import { Caption, Row, Stage, StageLabel, ValueCell } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/**
 * POINTER: the same row as ARRAY, but the pointers are the subject of the step, so they are
 * labelled above and below the row to show both ends.
 */
export function PointerRenderer({ payload }: RendererProps) {
  const values = readCellValues(payload, "values")
  const highlights = new Set(readIndexes(payload, "highlight"))
  const pointers = readPointers(payload)
  const above = pointers.filter((pointer) => pointer.name === "left" || pointer.name === "i")
  const below = pointers.filter((pointer) => !above.includes(pointer))
  const aboveByIndex = new Map(above.map((pointer) => [pointer.index, pointer.name]))
  const belowByIndex = new Map(below.map((pointer) => [pointer.index, pointer.name]))

  return (
    <Stage label="Two pointer visualisation">
      <div>
        <StageLabel>pointers</StageLabel>
        <Row>
          {values.map((value, index) => (
            <div key={index} className="flex flex-col items-center gap-1">
              <div className="h-4 font-mono text-[11px] text-muted-foreground">
                {aboveByIndex.get(index)}
              </div>
              <ValueCell tone={highlights.has(index) ? "pointer" : "default"}>{value}</ValueCell>
              <div className="h-4 font-mono text-[11px] text-muted-foreground">
                {belowByIndex.get(index)}
              </div>
            </div>
          ))}
        </Row>
        {pointers.length > 0 && (
          <Caption>
            {pointers.map((pointer) => `${pointer.name} = ${pointer.index}`).join("   ")}
          </Caption>
        )}
      </div>
    </Stage>
  )
}