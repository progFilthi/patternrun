import { readNumber, readString, readStringArray } from "@/components/animation/payload"
import { cn } from "@/lib/utils"
import { Stage, StageLabel } from "@/components/animation/primitives"
import type { RendererProps } from "@/components/animation/types"

/**
 * QUESTION: the predict-the-move mechanic (README section 8). The learner must answer before
 * the stage can continue, which is what makes it a prediction and not a quiz at the end.
 */
export function QuestionRenderer({ payload, context }: RendererProps) {
  const prompt = readString(payload, "prompt")
  const options = readStringArray(payload, "options")
  const answerIndex = readNumber(payload, "answerIndex", -1)
  const explanation = readString(payload, "explanation")
  const answered = context.answer !== null
  const correct = context.answer === answerIndex

  return (
    <Stage label="Prediction question">
      <div className="w-full max-w-xl self-stretch text-left">
        <StageLabel>predict the next move</StageLabel>
        <p className="text-[15px] leading-relaxed">{prompt}</p>

        <div className="mt-4 flex flex-col gap-2">
          {options.map((option, index) => {
            const isChosen = context.answer === index
            const showCorrect = answered && index === answerIndex
            const showWrong = answered && isChosen && !correct
            return (
              <button
                key={index}
                type="button"
                disabled={answered}
                onClick={() => context.onAnswer(index)}
                className={cn(
                  "flex items-center gap-3 rounded-md border px-3 py-2 text-left text-sm transition-colors",
                  "disabled:cursor-default",
                  showCorrect
                    ? "border-success bg-success/5 text-foreground"
                    : showWrong
                      ? "border-danger bg-danger/5 text-foreground"
                      : isChosen
                        ? "border-foreground"
                        : "border-border hover:border-foreground/40 hover:bg-secondary",
                )}
              >
                <span className="font-mono text-xs text-muted-foreground">
                  {String.fromCharCode(65 + index)}
                </span>
                <span>{option}</span>
              </button>
            )
          })}
        </div>

        {answered && (
          <p className={cn("mt-4 text-sm", correct ? "text-success" : "text-danger")}>
            {correct ? "Correct." : "Not quite."}{" "}
            <span className="text-muted-foreground">{explanation}</span>
          </p>
        )}
      </div>
    </Stage>
  )
}