import type { AnimationPayload } from "@/types/api"

export interface RendererContext {
  /** Option index the learner picked for a QUESTION step, or null when unanswered. */
  answer: number | null
  onAnswer: (optionIndex: number) => void
}

export interface RendererProps {
  payload: AnimationPayload
  context: RendererContext
}