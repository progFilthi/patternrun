"use client"

import { useEffect } from "react"
import { Moon, Sun } from "lucide-react"

import { THEME_STORAGE_KEY, applyTheme, appliedTheme, storedTheme, systemTheme } from "@/lib/theme"

/**
 * Light/dark switch.
 *
 * The icon and its label are chosen by CSS from the `dark` class on <html>, not by React
 * state. That keeps the server markup and the first client render identical whatever is in
 * localStorage, so there is no hydration mismatch and no flash. The click handler reads the
 * class the init script already put on the document.
 */
export function ThemeToggle() {
  // With no explicit choice stored, keep following the OS as it changes.
  useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)")
    const onChange = () => {
      if (storedTheme() === null) applyTheme(systemTheme())
    }
    media.addEventListener("change", onChange)
    return () => media.removeEventListener("change", onChange)
  }, [])

  // A second tab toggling the theme should not leave this one stale.
  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key === null || event.key === THEME_STORAGE_KEY) {
        const stored = storedTheme()
        if (stored) applyTheme(stored)
      }
    }
    window.addEventListener("storage", onStorage)
    return () => window.removeEventListener("storage", onStorage)
  }, [])

  return (
    <button
      type="button"
      onClick={() => applyTheme(appliedTheme() === "dark" ? "light" : "dark")}
      title="Switch between light and dark"
      className="group relative inline-flex size-9 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="sr-only">
        <span className="dark:hidden">Switch to dark theme</span>
        <span className="hidden dark:inline">Switch to light theme</span>
      </span>
      <Sun
        aria-hidden
        className="size-[18px] transition-transform duration-300 group-hover:rotate-45 dark:hidden"
      />
      <Moon
        aria-hidden
        className="hidden size-[18px] transition-transform duration-300 group-hover:-rotate-12 dark:block"
      />
    </button>
  )
}