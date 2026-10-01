"use client"

import { useSyncExternalStore } from "react"

import { completedSlugs, recordCompletion } from "@/lib/training/progress"
import type { CompletionRecord } from "@/lib/training/progress"

const EMPTY: ReadonlySet<string> = new Set<string>()

const listeners = new Set<() => void>()
let cache: ReadonlySet<string> | null = null

function getSnapshot(): ReadonlySet<string> {
  cache ??= completedSlugs()
  return cache
}

function getServerSnapshot(): ReadonlySet<string> {
  return EMPTY
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/**
 * Completion state for list screens. `useSyncExternalStore` keeps the server render and the
 * first client render identical, then reads localStorage after hydration.
 */
export function useCompletedSlugs(): ReadonlySet<string> {
  return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)
}

export function saveCompletion(slug: string, record: CompletionRecord): void {
  recordCompletion(slug, record)
  cache = completedSlugs()
  listeners.forEach((listener) => listener())
}