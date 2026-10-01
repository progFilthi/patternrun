import { Badge } from "@/components/ui/badge"
import type { TrainingDifficulty } from "@/types/api"

const TRAINING_LABEL: Record<TrainingDifficulty, string> = {
  RECOGNITION: "Recognition",
  GUIDED: "Guided",
  INDEPENDENT: "Independent",
  SPEEDRUN: "Speedrun",
  BOSS: "Boss",
}

export function TrainingDifficultyBadge({ value }: { value: TrainingDifficulty }) {
  return (
    <Badge variant="secondary" className="font-normal">
      {TRAINING_LABEL[value]}
    </Badge>
  )
}