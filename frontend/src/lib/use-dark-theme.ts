"use client"

import { useSyncExternalStore } from "react"

import { THEME_STORAGE_KEY } from "@/lib/theme"

/**
 * Whether the document is currently dark.
 *
 * The theme lives in a class on <html> that the init script sets before first paint, which is what
 * avoids a hydration mismatch. React still needs to know when it changes, so this subscribes to
 * that one attribute rather than to a second copy of the state — the toggle writes the class and
 * this follows, which is why nothing here has to know the toggle exists.
 *
 * `useSyncExternalStore` rather than an effect plus state: the theme can change before this
 * component mounts, and a subscription returns the right answer immediately where an effect would
 * render once in the wrong theme first.
 */
const darkQuery = () => document.documentElement.classList.contains("dark")

function subscribe(onChange: () => void) {
  const observer = new MutationObserver(onChange)
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ["class"] })

  // Another tab switching the theme writes localStorage but not this document's class, so the
  // storage event is the only signal that one arrives here.
  const onStorage = (event: StorageEvent) => {
    if (event.key === null || event.key === THEME_STORAGE_KEY) onChange()
  }
  window.addEventListener("storage", onStorage)

  return () => {
    observer.disconnect()
    window.removeEventListener("storage", onStorage)
  }
}

export function useDarkTheme(): boolean {
  return useSyncExternalStore(subscribe, darkQuery, () => false)
}