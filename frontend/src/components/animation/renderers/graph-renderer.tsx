import { readCellRow, readCoordinate, readCoordinates } from "@/components/animation/payload"
import { Caption, Stage, StageLabel } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** GRAPH: a grid, with visited cells, the queue and the cell being processed. */
export function GraphRenderer({ payload }: RendererProps) {
  const rows = readCellRow(payload, "cells")
  const visited = new Set(readCoordinates(payload, "visited").map(([row, col]) => `${row},${col}`))
  const queued = new Set(readCoordinates(payload, "queue").map(([row, col]) => `${row},${col}`))
  const current = readCoordinate(payload, "current")

  return (
    <Stage label="Grid visualisation">
      <div>
        <StageLabel>grid</StageLabel>
        <div
          className="grid gap-1"
          style={{ gridTemplateColumns: `repeat(${Math.max(rows[0]?.length ?? 1, 1)}, minmax(2rem, auto))` }}
        >
          {rows.flatMap((row, rowIndex) =>
            row.map((cell, colIndex) => {
              const key = `${rowIndex},${colIndex}`
              const isCurrent = current?.[0] === rowIndex && current?.[1] === colIndex
              const tone = isCurrent
                ? "bg-foreground text-background border-foreground"
                : visited.has(key)
                  ? "bg-foreground/15 border-foreground/40"
                  : queued.has(key)
                    ? "border-dashed border-foreground/60"
                    : "bg-card"
              return (
                <span
                  key={key}
                  className={`flex h-9 items-center justify-center rounded border font-mono text-sm transition-colors duration-200 ${tone}`}
                >
                  {cell}
                </span>
              )
            }),
          )}
        </div>
        <Caption>
          visited {visited.size}  ·  queued {queued.size}
          {current ? `  ·  current [${current[0]}, ${current[1]}]` : ""}
        </Caption>
      </div>
    </Stage>
  )
}