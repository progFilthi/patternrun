import { readCellValues, readIndexes, readNumberArray } from "@/components/animation/payload"
import { Caption, Row, Stage, StageLabel, ValueCell } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** TREE: values plus a parent index per node, which is enough to draw the levels. */
export function TreeRenderer({ payload }: RendererProps) {
  const values = readCellValues(payload, "values")
  const parents = readNumberArray(payload, "parents")
  const highlights = new Set(readIndexes(payload, "highlight"))

  const levels: number[][] = []
  values.forEach((_, index) => {
    const parent = parents[index] ?? -1
    const depth = parent < 0 ? 0 : (parents[parent] < 0 ? 1 : depthOf(index, parents))
    ;(levels[depth] ??= []).push(index)
  })

  return (
    <Stage label="Tree visualisation">
      <div>
        <StageLabel>tree</StageLabel>
        <div className="flex flex-col gap-3">
          {levels.map((level, depth) => (
            <Row key={depth} className="justify-center">
              {level.map((index) => (
                <ValueCell key={index} tone={highlights.has(index) ? "highlight" : "default"}>
                  {values[index]}
                </ValueCell>
              ))}
            </Row>
          ))}
        </div>
        <Caption>highlighted: {[...highlights].join(", ") || "none"}</Caption>
      </div>
    </Stage>
  )
}

function depthOf(index: number, parents: number[]): number {
  let depth = 0
  let current = index
  const guard = new Set<number>()
  while (current >= 0 && !guard.has(current)) {
    guard.add(current)
    current = parents[current] ?? -1
    depth += 1
  }
  return Math.max(depth - 1, 0)
}