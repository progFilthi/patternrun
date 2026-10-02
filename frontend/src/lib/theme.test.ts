import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import {
  THEME_STORAGE_KEY,
  applyTheme,
  appliedTheme,
  storedTheme,
  systemTheme,
} from "@/lib/theme"

/**
 * Theme lives on the document element, so these tests drive the real thing: the class, the
 * inline color-scheme, and the stored choice. The init script is what runs before paint in the
 * browser, and it reads the same storage key.
 */

/** The live storage implementation, which the test environment supplies. */
function storagePrototype(): Storage {
  return Object.getPrototypeOf(window.localStorage) as Storage
}

function prefersDark(value: boolean) {
  return vi.fn().mockReturnValue({
    matches: value,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })
}

beforeEach(() => {
  window.localStorage.clear()
  document.documentElement.classList.remove("dark")
  document.documentElement.style.colorScheme = ""
})

afterEach(() => {
  window.localStorage.clear()
})

describe("applyTheme", () => {
  it("adds the dark class and tells the document its colour scheme", () => {
    applyTheme("dark")

    expect(document.documentElement.classList.contains("dark")).toBe(true)
    expect(document.documentElement.style.colorScheme).toBe("dark")
  })

  it("removes the dark class for light", () => {
    applyTheme("dark")
    applyTheme("light")

    expect(document.documentElement.classList.contains("dark")).toBe(false)
    expect(document.documentElement.style.colorScheme).toBe("light")
  })

  it("persists the choice so it survives the next page view", () => {
    applyTheme("dark")
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe("dark")

    applyTheme("light")
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe("light")
  })

  it("still applies the theme when storage is unavailable", () => {
    // Private mode or a full quota must not stop the page from being readable.
    const setItem = vi
      .spyOn(storagePrototype(), "setItem")
      .mockImplementation(() => {
        throw new Error("QuotaExceededError")
      })

    expect(() => applyTheme("dark")).not.toThrow()
    expect(document.documentElement.classList.contains("dark")).toBe(true)

    setItem.mockRestore()
  })
})

describe("appliedTheme", () => {
  it("reports the class the document is actually carrying", () => {
    applyTheme("dark")
    expect(appliedTheme()).toBe("dark")

    applyTheme("light")
    expect(appliedTheme()).toBe("light")
  })
})

describe("storedTheme", () => {
  it("reads a stored choice back", () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, "dark")
    expect(storedTheme()).toBe("dark")

    window.localStorage.setItem(THEME_STORAGE_KEY, "light")
    expect(storedTheme()).toBe("light")
  })

  it("reports no choice when nothing is stored", () => {
    // No stored choice is what makes the app follow the OS instead.
    expect(storedTheme()).toBeNull()
  })

  it("treats an unrecognised value as no choice rather than trusting it", () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, "sepia")
    expect(storedTheme()).toBeNull()
  })

  it("treats an empty value as no choice", () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, "")
    expect(storedTheme()).toBeNull()
  })

  it("reports no choice when reading storage throws", () => {
    const getItem = vi
      .spyOn(storagePrototype(), "getItem")
      .mockImplementation(() => {
        throw new Error("SecurityError")
      })

    expect(storedTheme()).toBeNull()

    getItem.mockRestore()
  })
})

describe("systemTheme", () => {
  it("follows an OS that prefers dark", () => {
    vi.stubGlobal("matchMedia", prefersDark(true))
    expect(systemTheme()).toBe("dark")
  })

  it("follows an OS that prefers light", () => {
    vi.stubGlobal("matchMedia", prefersDark(false))
    expect(systemTheme()).toBe("light")
  })
})