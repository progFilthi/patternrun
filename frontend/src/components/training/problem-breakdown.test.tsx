import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { ProblemBreakdown } from "@/components/training/problem-breakdown"

/**
 * The reading step.
 *
 * The load-time assertions matter as much as the submit-time ones: a breakdown whose answer key
 * arrived with the options would be decoration, so the read is checked as carefully as the post.
 */

const PROMPTS = [
  {
    key: "given",
    prompt: "What are you handed?",
    options: ["An array of integers and one target integer", "A sorted array of pairs"],
  },
  {
    key: "asked",
    prompt: "What must you hand back?",
    options: ["The two values that add up", "The indices of the two values that add up"],
  },
]

const VERDICT = {
  correct: true,
  answered: 2,
  total: 2,
  prompts: [
    {
      key: "given",
      prompt: "What are you handed?",
      chosenIndex: 0,
      correct: true,
      explanation: "One array, nums, plus a single target.",
    },
    {
      key: "asked",
      prompt: "What must you hand back?",
      chosenIndex: 1,
      correct: true,
      explanation: "Indices, not values.",
    },
  ],
}

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith("/breakdown")) {
        return new Response(JSON.stringify({ problemSlug: "two-sum", prompts: PROMPTS }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        })
      }
      return new Response(JSON.stringify({ message: "unexpected" }), { status: 404 })
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

/** Renders and waits for the prompts, leaving the step collapsed as it starts in real use. */
async function renderCollapsed(
  props: Partial<Parameters<typeof ProblemBreakdown>[0]> = {},
) {
  const onSubmit = vi.fn()
  render(
    <ProblemBreakdown
      slug="two-sum"
      pending={false}
      onSubmit={onSubmit}
      {...props}
    />,
  )
  // The heading is the one thing rendered in every state, so it is the honest signal that
  // the load has settled and the component is ready to be driven.
  await screen.findByRole("heading", { name: /before you pick a pattern/i })
  return { onSubmit }
}

/** Renders, waits for the load, then opens the step the way a learner would. */
async function renderOpen(props: Partial<Parameters<typeof ProblemBreakdown>[0]> = {}) {
  const handle = await renderCollapsed(props)
  if (props.submitted) {
    // A verdict can render before the options arrive; the tests below assert on the options,
    // so wait for the load to finish rather than racing it.
    await screen.findAllByRole("radiogroup")
  } else {
    fireEvent.click(screen.getByRole("button", { name: /show/i }))
  }
  return handle
}

describe("loading", () => {
  it("renders the prompts the API returned", async () => {
    await renderOpen()

    expect(screen.getByText("What are you handed?")).toBeDefined()
    expect(screen.getByText("What must you hand back?")).toBeDefined()
    expect(screen.getAllByRole("radiogroup")).toHaveLength(2)
  })

  it("starts collapsed so reading the statement comes first", async () => {
    await renderCollapsed()

    // Nothing on screen until asked. The statement is the thing to read first; a three-question
    // quiz sitting above it would invert the order the step is trying to teach.
    expect(screen.queryByRole("radiogroup")).toBeNull()
    expect(screen.getByRole("button", { name: /show/i })).toBeDefined()
  })

  it("renders nothing at all when the problem has no breakdown", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify({ message: "No breakdown" }), { status: 404 })),
    )
    const { container } = render(
      <ProblemBreakdown slug="climbing-stairs" pending={false} onSubmit={() => {}} />,
    )

    await waitFor(() => expect(container.innerHTML).toBe(""))
  })
})

describe("answering", () => {
  it("sends keys and chosen indices and nothing else", async () => {
    const { onSubmit } = await renderOpen()

    fireEvent.click(screen.getAllByRole("radio")[0]) // given / 0
    fireEvent.click(screen.getAllByRole("radio")[3]) // asked / 1

    fireEvent.click(screen.getByRole("button", { name: /check my reading/i }))

    expect(onSubmit).toHaveBeenCalledWith([
      { key: "given", chosenIndex: 0 },
      { key: "asked", chosenIndex: 1 },
    ])
    // The wire assertion, in component form: the payload is a list of choices.
    for (const answer of onSubmit.mock.calls[0][0]) {
      expect(Object.keys(answer).sort()).toEqual(["chosenIndex", "key"])
    }
  })

  it("will not submit until every prompt is answered", async () => {
    const { onSubmit } = await renderOpen()
    fireEvent.click(screen.getAllByRole("radio")[0])

    expect(screen.getByRole("button", { name: /check my reading/i })).toHaveProperty(
      "disabled",
      true,
    )
    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText("1 of 2 answered.")).toBeDefined()
  })

  it("disables the control while the check is in flight", async () => {
    await renderOpen({ pending: true })
    expect(screen.getByRole("button", { name: /checking/i })).toHaveProperty("disabled", true)
  })
})

describe("after the verdict", () => {
  it("shows the explanations the server sent", async () => {
    await renderOpen({ submitted: VERDICT })

    expect(screen.getByText(/One array, nums, plus a single target/)).toBeDefined()
    expect(screen.getByText(/Indices, not values/)).toBeDefined()
  })

  it("locks the answers, so a verdict cannot be revised by re-picking", async () => {
    await renderOpen({ submitted: VERDICT })

    const radios = screen.getAllByRole("radio")
    expect(radios.every((radio) => (radio as HTMLButtonElement).disabled)).toBe(true)
  })

  it("marks a fully correct reading plainly", async () => {
    await renderOpen({ submitted: VERDICT })

    expect(screen.getByText(/read it correctly/i)).toBeDefined()
    // One mark per answered prompt, not one for the step as a whole: partial credit is not a
    // thing here, but the learner still deserves to see which reading was right.
    expect(screen.getAllByLabelText("correct")).toHaveLength(2)
  })

  it("marks the wrong choice without shaming the learner", async () => {
    await renderOpen({
      submitted: {
        ...VERDICT,
        correct: false,
        prompts: [{ ...VERDICT.prompts[0], chosenIndex: 1, correct: false }, VERDICT.prompts[1]],
      },
    })

    expect(screen.getByLabelText("not what the problem asked for")).toBeDefined()
    // The wording points at the statement, not at the learner.
    expect(screen.getByText(/another look at the statement/i)).toBeDefined()
  })

  it("offers no way to resubmit once judged", async () => {
    await renderOpen({ submitted: VERDICT })

    expect(screen.queryByRole("button", { name: /check my reading/i })).toBeNull()
  })
})