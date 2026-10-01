import Link from "next/link"
import { ChevronRight } from "lucide-react"

import { cn } from "@/lib/utils"

export type Crumb = {
  label: string
  /** The last crumb is the current page and is not a link. */
  href?: string
}

/**
 * Where you are in the library, as a trail. On a problem page this answers "which pattern is
 * this, and how do I get back to it" without relying on browser history.
 */
export function Breadcrumb({ crumbs, className }: { crumbs: Crumb[]; className?: string }) {
  return (
    <nav aria-label="Breadcrumb" className={cn("min-w-0", className)}>
      <ol className="flex flex-wrap items-center gap-1 text-xs">
        {crumbs.map((crumb, index) => {
          const last = index === crumbs.length - 1
          return (
            <li key={`${crumb.label}-${index}`} className="flex min-w-0 items-center gap-1">
              {index > 0 && (
                <ChevronRight aria-hidden className="size-3 shrink-0 text-muted-foreground/50" />
              )}
              {crumb.href && !last ? (
                <Link
                  href={crumb.href}
                  className="truncate rounded-sm text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                >
                  {crumb.label}
                </Link>
              ) : (
                <span
                  aria-current={last ? "page" : undefined}
                  className={cn("truncate", last ? "text-foreground" : "text-muted-foreground")}
                >
                  {crumb.label}
                </span>
              )}
            </li>
          )
        })}
      </ol>
    </nav>
  )
}