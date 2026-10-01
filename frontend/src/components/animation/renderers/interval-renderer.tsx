import { readIntervals, readNumber } from "@/components/animation/payload"
import { Caption, Stage, StageLabel } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/** INTERVAL: intervals on a shared number line, with the merged result underneath. */
export function IntervalRenderer({ payload }: RendererProps) {
  const intervals = readIntervals(payload, "intervals")
  const merged = readIntervals(payload, "merged")
  const highlight = readNumber(payload, "highlight", -1)

  const all = [...intervals, ...merged]
  const min = all.length > 0 ? Math.min(...all.map((interval) => interval.start)) : 0
  const max = all.length > 0 ? Math.max(...all.map((interval) => interval.end)) : 1
  const span = Math.max(max - min, 1)
  const width = (interval: { start: number; end: number }) =>
    `${Math.max(((interval.end - interval.start) / span) * 100, 6)}%`

  return (
    <Stage label="Interval visualisation">
      <div className="w-full max-w-2xl">
        <StageLabel>intervals</StageLabel>
        <div className="flex flex-col gap-1.5">
          {intervals.map((interval, index) => (
            <div key={`${interval.start}-${interval.end}-${index}`} className="relative h-6">
              <div
                className={`absolute h-6 rounded border font-mono text-xs leading-6 transition-all duration-300 ${
                  index === highlight
                    ? "border-foreground bg-foreground text-background"
                    : "border-border bg-card"
                }`}
                style={{
                  left: `${((interval.start - min) / span) * 100}%`,
                  width: width(interval),
                }}
              >
                <span className="px-1.5">{`${interval.start}–${interval.end}`}</span>
              </div>
            </div>
          ))}
        </div>

        {merged.length > 0 && (
          <>
            <StageLabel>merged</StageLabel>
            <div className="flex flex-col gap-1.5">
              {merged.map((interval, index) => (
                <div key={`merged-${interval.start}-${interval.end}-${index}`} className="relative h-6">
                  <div
                    className="absolute h-6 rounded border border-foreground bg-foreground/10 font-mono text-xs leading-6 transition-all duration-300"
                    style={{
                      left: `${((interval.start - min) / span) * 100}%`,
                      width: width(interval),
                    }}
                  >
                    <span className="px-1.5">{`${interval.start}–${interval.end}`}</span>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
        <Caption>
          {intervals.length} input intervals · {merged.length} merged
        </Caption>
      </div>
    </Stage>
  )
}