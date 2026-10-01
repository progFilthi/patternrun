import type { AnimationPayload } from "@/types/api"

/**
 * Readers for the free form animation payload. Renderers must never crash on a partial
 * payload, so every reader returns a fallback instead of throwing.
 */

export function readNumber(payload: AnimationPayload, key: string, fallback = 0): number {
  const value = payload[key]
  if (typeof value === "number") return value
  if (typeof value === "string" && value.trim() !== "" && !Number.isNaN(Number(value))) {
    return Number(value)
  }
  return fallback
}

export function readOptionalNumber(payload: AnimationPayload, key: string): number | null {
  const value = payload[key]
  return typeof value === "number" && Number.isFinite(value) ? value : null
}

export function readString(payload: AnimationPayload, key: string, fallback = ""): string {
  const value = payload[key]
  return typeof value === "string" ? value : fallback
}

export function readBoolean(payload: AnimationPayload, key: string, fallback = false): boolean {
  const value = payload[key]
  return typeof value === "boolean" ? value : fallback
}

/** Values may be numbers or single character strings, depending on the problem. */
export function readCellValues(payload: AnimationPayload, key: string): Array<string | number> {
  const value = payload[key]
  if (!Array.isArray(value)) return []
  return value.filter((item): item is string | number =>
    typeof item === "number" || typeof item === "string",
  )
}

export function readNumberArray(payload: AnimationPayload, key: string): number[] {
  const value = payload[key]
  if (!Array.isArray(value)) return []
  return value.filter((item): item is number => typeof item === "number")
}

export function readStringArray(payload: AnimationPayload, key: string): string[] {
  const value = payload[key]
  if (!Array.isArray(value)) return []
  return value.filter((item): item is string => typeof item === "string")
}

export function readIndexes(payload: AnimationPayload, key: string): number[] {
  return readNumberArray(payload, key)
}

/** Named indices such as `{ left: 0, right: 8 }`. */
export function readPointers(payload: AnimationPayload): Array<{ name: string; index: number }> {
  const value = payload.pointers
  if (typeof value !== "object" || value === null || Array.isArray(value)) return []
  return Object.entries(value)
    .filter((entry): entry is [string, number] => typeof entry[1] === "number")
    .map(([name, index]) => ({ name, index }))
    .sort((left, right) => left.index - right.index)
}

export interface Pair {
  key: string | number
  value: string | number
}

/** Hash map entries arrive as `[{ key, value }]`. */
export function readEntries(payload: AnimationPayload): Pair[] {
  const value = payload.entries
  if (!Array.isArray(value)) return []
  return value.flatMap((entry) => {
    if (typeof entry !== "object" || entry === null) return []
    const { key, value: entryValue } = entry as Record<string, unknown>
    if (key === undefined || entryValue === undefined) return []
    if (typeof key !== "string" && typeof key !== "number") return []
    if (typeof entryValue !== "string" && typeof entryValue !== "number") return []
    return [{ key, value: entryValue }]
  })
}

export function readLookup(payload: AnimationPayload): { key: string | number; found: boolean } | null {
  const value = payload.lookup
  if (typeof value !== "object" || value === null || Array.isArray(value)) return null
  const { key, found } = value as Record<string, unknown>
  if (typeof key !== "string" && typeof key !== "number") return null
  return { key, found: found === true }
}

export interface Interval {
  start: number
  end: number
}

/** Intervals arrive as `[start, end]` pairs. */
export function readIntervals(payload: AnimationPayload, key: string): Interval[] {
  const value = payload[key]
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    if (!Array.isArray(item) || item.length < 2) return []
    const [start, end] = item
    if (typeof start !== "number" || typeof end !== "number") return []
    return [{ start, end }]
  })
}

export function readCellRow(payload: AnimationPayload, key: string): Array<Array<string | number>> {
  const value = payload[key]
  if (!Array.isArray(value)) return []
  return value
    .filter((row): row is unknown[] => Array.isArray(row))
    .map((row) =>
      row.filter((cell): cell is string | number =>
        typeof cell === "number" || typeof cell === "string",
      ),
    )
}

/** `current` is a single `[row, col]` pair, unlike `visited` and `queue` which are lists. */
export function readCoordinate(payload: AnimationPayload, key: string): [number, number] | null {
  const value = payload[key]
  if (!Array.isArray(value) || value.length < 2) return null
  const [row, col] = value
  if (typeof row !== "number" || typeof col !== "number") return null
  return [row, col]
}

export function readCoordinates(payload: AnimationPayload, key: string): Array<[number, number]> {
  const value = payload[key]
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    if (!Array.isArray(item) || item.length < 2) return []
    const [row, col] = item
    if (typeof row !== "number" || typeof col !== "number") return []
    return [[row, col] as [number, number]]
  })
}

export function readHeapItems(payload: AnimationPayload): Array<Record<string, string | number>> {
  const value = payload.items
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    if (typeof item !== "object" || item === null || Array.isArray(item)) return []
    const entries = Object.entries(item as Record<string, unknown>).filter(
      (entry): entry is [string, string | number] =>
        typeof entry[1] === "string" || typeof entry[1] === "number",
    )
    return entries.length > 0 ? [Object.fromEntries(entries)] : []
  })
}

export function isEmptyPayload(payload: AnimationPayload): boolean {
  return Object.keys(payload).length === 0
}