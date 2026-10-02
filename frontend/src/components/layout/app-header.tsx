"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"

import { ThemeToggle } from "@/components/layout/theme-toggle"

const NAV_ITEMS = [
  { href: "/", label: "Dashboard" },
  { href: "/patterns", label: "Patterns" },
  { href: "/problems", label: "Problems" },
  { href: "/progress", label: "Progress" },
] as const

export function AppHeader() {
  const pathname = usePathname()

  return (
    <header className="sticky top-0 z-40 border-b border-border/80 bg-background/85 backdrop-blur-md">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center gap-3 px-4 sm:gap-6 sm:px-6">
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:absolute focus:top-3 focus:left-4 focus:z-50 focus:rounded-md focus:bg-secondary focus:px-3 focus:py-2 focus:text-sm focus:font-medium"
        >
          Skip to content
        </a>

        <Link
          href="/"
          className="flex shrink-0 items-center gap-2.5 rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <BrandMark />
          {/* Below sm the three nav labels need the room more than the wordmark does. */}
          <span className="hidden text-[15px] font-semibold tracking-tight text-foreground sm:inline">
            PatternRun
          </span>
          <span className="hidden text-xs text-muted-foreground xl:inline">
            Recognize. Solve. Explain.
          </span>
        </Link>

        <nav aria-label="Main" className="flex flex-1 justify-center">
          <ul className="flex items-center gap-0.5 sm:gap-1">
            {NAV_ITEMS.map((item) => {
              const active =
                item.href === "/" ? pathname === "/" : pathname.startsWith(item.href)
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    className={`relative inline-flex h-9 items-center rounded-lg px-2.5 text-sm font-medium transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50 sm:px-3.5 sm:text-[15px] ${
                      active
                        ? "text-foreground after:absolute after:inset-x-2 after:bottom-0.5 after:h-0.5 after:rounded-full after:bg-foreground"
                        : "text-muted-foreground hover:bg-muted hover:text-foreground"
                    }`}
                  >
                    {item.label}
                  </Link>
                </li>
              )
            })}
          </ul>
        </nav>

        <div className="flex shrink-0 items-center gap-1">
          <ThemeToggle />
        </div>
      </div>
    </header>
  )
}

/** A frame with a window inside it: the sliding window pattern, in one glyph. */
function BrandMark() {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="none"
      aria-hidden
      className="size-[22px] shrink-0 text-foreground transition-colors"
    >
      <rect
        x="1.75"
        y="3.75"
        width="16.5"
        height="12.5"
        rx="3.25"
        stroke="currentColor"
        strokeWidth="1.5"
      />
      <rect x="5.5" y="7" width="9" height="6" rx="1.75" fill="currentColor" />
    </svg>
  )
}