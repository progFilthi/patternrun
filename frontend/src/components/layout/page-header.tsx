import type { ReactNode } from "react"

import { cn } from "@/lib/utils"

/**
 * The masthead shared by every top level page: a small mono eyebrow, the page title, and an
 * optional lede. Keeping it in one place is what makes the hierarchy consistent across
 * routes instead of each page inventing its own spacing.
 */
export function PageHeader({
  eyebrow,
  title,
  lede,
  actions,
  className,
}: {
  eyebrow: string
  title: string
  lede?: ReactNode
  actions?: ReactNode
  className?: string
}) {
  return (
    <header className={cn("flex flex-col gap-5", className)}>
      <div className="flex flex-col gap-3">
        <Eyebrow>{eyebrow}</Eyebrow>
        <h1 className="text-3xl font-medium tracking-tight text-balance sm:text-4xl">{title}</h1>
      </div>
      {lede ? (
        <p className="max-w-2xl text-[15px] leading-relaxed text-muted-foreground">{lede}</p>
      ) : null}
      {actions ? <div className="flex flex-wrap items-center gap-3">{actions}</div> : null}
    </header>
  )
}

/** Mono, uppercase, quiet. Labels the section above it, never competes with it. */
export function Eyebrow({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <p
      className={cn(
        "font-mono text-[11px] uppercase tracking-[0.08em] text-muted-foreground",
        className,
      )}
    >
      {children}
    </p>
  )
}

/**
 * A titled band with a hairline underneath and an optional trailing action, so section
 * boundaries are visible at a glance instead of relying on whitespace alone.
 */
export function Section({
  title,
  meta,
  action,
  lede,
  className,
  children,
}: {
  title: string
  meta?: ReactNode
  action?: ReactNode
  /** One sentence under the title. Explains the section rather than restating it. */
  lede?: ReactNode
  className?: string
  children: ReactNode
}) {
  return (
    <section className={className}>
      <div className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-1 border-b pb-4">
        <h2 className="text-sm font-medium tracking-tight">{title}</h2>
        {/* Both, not `action ?? meta`. `??` made `meta` render only when there was no action, so a
            section passing both silently lost its meta on every page. */}
        <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
          {meta ? <span className="text-xs text-muted-foreground">{meta}</span> : null}
          {action}
        </div>
      </div>
      {lede ? (
        <>
          <p className="mt-3 max-w-3xl text-sm text-muted-foreground">{lede}</p>
          <div className="mt-4">{children}</div>
        </>
      ) : (
        children
      )}
    </section>
  )
}
