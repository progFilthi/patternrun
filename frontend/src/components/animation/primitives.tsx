import { cn } from "@/lib/utils"

/**
 * Shared visual primitives for every animation renderer. Renderers compose these so the
 * motion language stays identical across problem types.
 */

export function Stage({
  children,
  className,
  label,
}: {
  children: React.ReactNode
  className?: string
  label: string
}) {
  return (
    <div
      role="group"
      aria-label={label}
      className={cn(
        "min-h-44 rounded-md border bg-surface p-6",
        "flex items-center justify-center overflow-x-auto",
        className,
      )}
    >
      {children}
    </div>
  )
}

export function StageLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="mb-3 text-[11px] font-medium uppercase tracking-[0.08em] text-muted-foreground">
      {children}
    </p>
  )
}

/** A single value in a row: the array, window, tree and table renderers all use these. */
export function ValueCell({
  children,
  tone = "default",
  className,
}: {
  children: React.ReactNode
  tone?: "default" | "highlight" | "window" | "muted" | "pointer" | "visited"
  className?: string
}) {
  return (
    <span
      className={cn(
        "inline-flex h-9 min-w-9 items-center justify-center rounded border px-2 font-mono text-sm",
        "transition-colors duration-200",
        tone === "default" && "border-border bg-card text-foreground",
        tone === "muted" && "border-border bg-card text-muted-foreground",
        tone === "highlight" && "border-foreground/40 bg-foreground text-background",
        tone === "window" && "border-foreground bg-foreground/10 text-foreground",
        tone === "pointer" && "border-foreground bg-foreground text-background",
        tone === "visited" && "border-border bg-foreground/10 text-muted-foreground",
        className,
      )}
    >
      {children}
    </span>
  )
}

export function PointerTag({ name, tone = "pointer" }: { name: string; tone?: "pointer" | "window" }) {
  return (
    <span
      className={cn(
        "rounded px-1.5 py-0.5 font-mono text-[11px]",
        tone === "pointer" ? "bg-foreground text-background" : "bg-foreground/10 text-foreground",
      )}
    >
      {name}
    </span>
  )
}

export function Row({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={cn("flex flex-wrap items-center gap-1.5", className)}>{children}</div>
}

export function Column({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={cn("flex flex-col gap-2", className)}>{children}</div>
}

/** Vertical list rendered top to bottom, used by STACK. */
export function StackColumn({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex w-40 flex-col-reverse gap-1.5">
      {children}
    </div>
  )
}

export function Caption({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <p className={cn("mt-4 text-center font-mono text-xs text-muted-foreground", className)}>
      {children}
    </p>
  )
}

export function EmptyPayload({ message }: { message?: string }) {
  return (
    <p className="text-sm text-muted-foreground">
      {message ?? "This step has no visual payload. Read the description above."}
    </p>
  )
}

export function StateLabel({ children, tone = "muted" }: { children: React.ReactNode; tone?: "muted" | "success" | "danger" }) {
  return (
    <span
      className={cn(
        "font-mono text-xs uppercase tracking-wide",
        tone === "muted" && "text-muted-foreground",
        tone === "success" && "text-success",
        tone === "danger" && "text-danger",
      )}
    >
      {children}
    </span>
  )
}