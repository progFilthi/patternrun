import { readHeapItems, readNumber } from "@/components/animation/payload"
import { Caption, Row, Stage, StageLabel, ValueCell } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** HEAP: the k elements the algorithm decided are worth keeping. */
export function HeapRenderer({ payload }: RendererProps) {
  const items = readHeapItems(payload)
  const size = readNumber(payload, "size", items.length)

  return (
    <Stage label="Heap visualisation">
      <div>
        <StageLabel>heap</StageLabel>
        <Row>
          {items.map((item, index) => (
            <ValueCell key={index} tone={index === 0 ? "pointer" : "default"}>
              {Object.entries(item)
                .map(([key, value]) => `${key} ${value}`)
                .join("  ")}
            </ValueCell>
          ))}
        </Row>
        <Caption>
          size = {size}
          {items.length > 0 && "  ·  smallest first"}
        </Caption>
      </div>
    </Stage>
  )
}