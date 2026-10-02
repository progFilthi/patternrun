import { cleanup } from "@testing-library/react"
import { afterEach } from "vitest"

/**
 * Vitest's jsdom environment exposes `window.localStorage` as an empty object rather than a
 * working `Storage`: the instance jsdom builds for the window is replaced during global
 * population, and jsdom's `Storage` throws if constructed directly. Any component that reads or
 * writes a preference would otherwise fail every test for a reason that has nothing to do with
 * the component.
 *
 * A per-file in-memory implementation keeps those tests honest and deterministic. A shared
 * browser `localStorage` would also leak state between test files, which is the opposite of what
 * a test wants. Applied only when the environment's own implementation is unusable.
 */
class MemoryStorage implements Storage {
  #entries = new Map<string, string>()

  get length(): number {
    return this.#entries.size
  }

  key(index: number): string | null {
    return [...this.#entries.keys()][index] ?? null
  }

  getItem(key: string): string | null {
    return this.#entries.has(key) ? (this.#entries.get(key) as string) : null
  }

  setItem(key: string, value: string): void {
    this.#entries.set(String(key), String(value))
  }

  removeItem(key: string): void {
    this.#entries.delete(key)
  }

  clear(): void {
    this.#entries.clear()
  }
}

if (typeof window.localStorage?.getItem !== "function") {
  Object.defineProperty(window, "localStorage", {
    value: new MemoryStorage(),
    configurable: true,
    writable: true,
  })
}

/**
 * React Testing Library only registers its automatic cleanup when Vitest globals are enabled.
 * This suite imports `describe`/`it`/`expect` explicitly instead, so unmounting is wired up
 * here. Without it, every `render` in a file appends to the same document and queries that
 * expect a single element start failing for the wrong reason.
 */
afterEach(() => {
  cleanup()
})