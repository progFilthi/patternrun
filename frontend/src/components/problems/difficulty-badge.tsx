import { Badge } from "@/components/ui/badge"
import type { Difficulty } from "@/types/api"

const DIFFICULTY_LABEL: Record<Difficulty, string> = {
  EASY: "Easy",
  MEDIUM: "Medium",
  HARD: "Hard",
}

/** Difficulty is never communicated by colour alone (README section 56). */
export function DifficultyBadge({ difficulty }: { difficulty: Difficulty }) {
  return (
    <Badge variant="outline" className="gap-1 font-normal">
      <span
        aria-hidden
        className={
          difficulty === "EASY"
            ? "size-1.5 rounded-full bg-muted-foreground"
            : difficulty === "MEDIUM"
              ? "size-1.5 rounded-full bg-foreground"
              : "size-1.5 rounded-full bg-foreground ring-2 ring-foreground/30"
        }
      />
      {DIFFICULTY_LABEL[difficulty]}
    </Badge>
  )
}
