export type Theme = "light" | "dark"

/** Matches the versioned localStorage keys used elsewhere in the app. */
export const THEME_STORAGE_KEY = "patternrun.theme.v1"

/**
 * Runs before first paint, injected by the root layout.
 *
 * Theme has to be applied by the server-unknown document itself: reading it from
 * React would either flash the wrong theme or force a hydration mismatch, since
 * the server cannot know what is in localStorage or what the OS prefers.
 */
export const THEME_INIT_SCRIPT = `(function(){try{
var s=localStorage.getItem(${JSON.stringify(THEME_STORAGE_KEY)});
var d=s==="dark"||(s!=="light"&&matchMedia("(prefers-color-scheme: dark)").matches);
var e=document.documentElement;
e.classList.toggle("dark",d);
e.style.colorScheme=d?"dark":"light";
}catch(_){}})()`

function isTheme(value: string | null): value is Theme {
  return value === "light" || value === "dark"
}

/** The stored choice, or null when the user has never picked one. */
export function storedTheme(): Theme | null {
  try {
    const raw = window.localStorage.getItem(THEME_STORAGE_KEY)
    return isTheme(raw) ? raw : null
  } catch {
    return null
  }
}

/** What the document is actually showing right now. */
export function appliedTheme(): Theme {
  return document.documentElement.classList.contains("dark") ? "dark" : "light"
}

export function applyTheme(theme: Theme): void {
  const root = document.documentElement
  root.classList.toggle("dark", theme === "dark")
  root.style.colorScheme = theme
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, theme)
  } catch {
    // Private mode or a full quota: the theme still applies for this page view.
  }
}

/** The theme to use when the user has made no explicit choice. */
export function systemTheme(): Theme {
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"
}