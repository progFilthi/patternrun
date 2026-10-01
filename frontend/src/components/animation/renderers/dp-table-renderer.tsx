import { readCellRow, readCoordinate, readStringArray } from "@/components/animation/payload"
import { Caption, Stage, StageLabel } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** DP_TABLE: the state table, with the cell currently being filled highlighted. */
export function DpTableRenderer({ payload }: RendererProps) {
  const cells = readCellRow(payload, "cells")
  const rowLabels = readStringArray(payload, "rowLabels")
  const colLabels = readStringArray(payload, "colLabels")
  const current = readCoordinate(payload, "current")
  const currentRow = current?.[0]
  const currentCol = current?.[1]

  return (
    <Stage label="Dynamic programming table">
      <div className="overflow-x-auto">
        <StageLabel>state</StageLabel>
        <table className="border-collapse font-mono text-sm">
          <thead>
            <tr>
              <th className="px-2 py-1 text-left text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                &nbsp;
              </th>
              {colLabels.map((label, index) => (
                <th
                  key={`${label}-${index}`}
                  className="px-2 py-1 text-[11px] font-medium uppercase tracking-wide text-muted-foreground"
                >
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {cells.map((row, rowIndex) => (
              <tr key={rowIndex}>
                <th className="px-2 py-1 text-left text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                  {rowLabels[rowIndex] ?? ""}
                </th>
                {row.map((cell, colIndex) => {
                  const active = rowIndex === currentRow && colIndex === currentCol
                  return (
                    <td
                      key={colIndex}
                      className={`border px-3 py-1.5 text-center transition-colors duration-200 ${
                        active
                          ? "border-foreground bg-foreground text-background"
                          : "border-border"
                      }`}
                    >
                      {cell}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
        <Caption>
          {current ? `filling cell [${current[0]}, ${current[1]}]` : "table state"}
        </Caption>
      </div>
    </Stage>
  )
}