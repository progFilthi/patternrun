import { readEntries, readLookup } from "@/components/animation/payload"
import { Caption, Stage, StageLabel } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** HASH_MAP: the memory the algorithm has built so far, plus the current lookup. */
export function HashMapRenderer({ payload }: RendererProps) {
  const entries = readEntries(payload)
  const lookup = readLookup(payload)

  return (
    <Stage label="Hash map visualisation">
      <div className="w-full max-w-md">
        <StageLabel>seen</StageLabel>
        {entries.length === 0 ? (
          <p className="text-sm text-muted-foreground">empty</p>
        ) : (
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="text-left text-[11px] uppercase tracking-wide text-muted-foreground">
                <th className="border-b py-1.5 pr-4 font-medium">value</th>
                <th className="border-b py-1.5 font-medium">stored</th>
              </tr>
            </thead>
            <tbody className="font-mono">
              {entries.map((entry) => (
                <tr key={`${entry.key}-${entry.value}`}>
                  <td className="border-b py-1.5 pr-4">{entry.key}</td>
                  <td className="border-b py-1.5 text-muted-foreground">{entry.value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {lookup && (
          <Caption className="text-left">
            lookup {lookup.key} → {lookup.found ? "found" : "not found"}
          </Caption>
        )}
      </div>
    </Stage>
  )
}