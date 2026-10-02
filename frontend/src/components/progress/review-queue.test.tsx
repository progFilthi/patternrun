import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { ReviewQueue } from "@/components/progress/review-queue"
import { reviewMistake } from "@/lib/api/session"
import type { ReviewItem, ReviewOutcome } from "@/types/api"

vi.mock("@/lib/api/session", () => ({ reviewMistake: vi.fn() }))

const refreshMock = vi.fn()
vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh: refreshMock }) }))

/**
 * What the review queue does with a server answer.
 *
 * Two things are being defended here. The first is that this component never decides anything: the
 * interval it reports back and the XP it congratulates the learner on are read out of the response,
 * never worked out locally. The second is that a reviewed mistake leaves the queue without the
 * server having to be asked again, so a slow refresh cannot shuffle the card the learner is reading.
 */

function item(overrides: Partial<ReviewItem> = {}): ReviewItem {
  return {
    mistakeId: "m1",
    problemSlug: "two-sum",
    problemTitle: "Two Sum",
    category: "PATTERN",
    description: "Identified the pattern as sliding-window.",
    lesson: "Work from the constraints rather than the shape of the input.",
    reviewCount: 0,
    intervalDays: 0,
    dueAt: "2026-01-01T00:00:00Z",
    attemptsSince: 1,
    ...overrides,
  }
}

function outcome(overrides: Partial<ReviewOutcome> = {}): ReviewOutcome {
  return {
    mistakeId: "m1",
    problemSlug: "two-sum",
    category: "PATTERN",
    correct: true,
    reviewCount: 1,
    intervalDays: 2,
    nextDueAt: "2026-01-03T00:00:00Z",
    xpAwarded: 20,
    alreadyEarned: false,
    ...overrides,
  }
}

function text(container: HTMLElement) {
  return container.textContent ?? ""
}

function click(name: RegExp) {
  fireEvent.click(screen.getByRole("button", { name }))
}

beforeEach(() => {
  vi.clearAllMocks()
  refreshMock.mockReset()
})

describe("showing what is due", () => {
  it("shows the mistake and its lesson, not a question about whether it was right", () => {
    const { container } = render(<ReviewQueue items={[item()]} />)

    expect(text(container)).toContain(item().description)
    expect(text(container)).toContain(item().lesson)
    // The category is the learner's own label for the gap, worth showing as given.
    expect(text(container)).toContain("PATTERN")
  })

  it("says so plainly when nothing is due", () => {
    const { container } = render(<ReviewQueue items={[]} />)
    expect(text(container)).toContain("Nothing due for review")
  })

  it("counts how many times a returning mistake has already been seen", () => {
    const { container } = render(<ReviewQueue items={[item({ reviewCount: 3 })]} />)
    expect(text(container)).toContain("seen 3 times")
  })

  it("does not claim a first review has been seen before", () => {
    const { container } = render(<ReviewQueue items={[item({ reviewCount: 0 })]} />)
    expect(text(container)).not.toMatch(/seen \d+ time/)
  })

  it("admits when there are more behind this one, rather than hiding the queue length", () => {
    const { container } = render(<ReviewQueue items={[item(), item({ mistakeId: "m2" })]} />)
    expect(text(container)).toContain("2 due in total")
  })
})

describe("answering", () => {
  it("sends the answer and nothing else, since correctness is not the client's to claim", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome())

    render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() => expect(reviewMistake).toHaveBeenCalledWith("m1", true))
    // Exactly those two arguments: no interval, no XP, no correctness flag of our own.
    expect(vi.mocked(reviewMistake).mock.calls[0]).toEqual(["m1", true])
  })

  it("reports the interval the server chose, not one worked out here", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome({ intervalDays: 7, xpAwarded: 20 }))

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() => expect(text(container)).toContain("Back in 7 days"))
    expect(text(container)).toContain("+20 XP")
  })

  it("does not congratulate on XP the server declined to pay", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome({ xpAwarded: 0, alreadyEarned: true }))

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() => expect(text(container)).toContain("Reviewed."))
    expect(text(container)).not.toContain("+0 XP")
  })

  it("sends the same review whether the learner says they have it or not", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome({ correct: false, intervalDays: 1 }))

    render(<ReviewQueue items={[item()]} />)
    click(/still not/i)

    await waitFor(() => expect(reviewMistake).toHaveBeenCalledWith("m1", false))
  })

  it("keeps the mistake due sooner when the learner still does not have it", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome({ correct: false, intervalDays: 1 }))

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/still not/i)

    await waitFor(() => expect(text(container)).toContain("Back in 1 day."))
  })

  it("takes the reviewed card off the queue and shows the next one", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome())

    const { container } = render(
      <ReviewQueue
        items={[item(), item({ mistakeId: "m2", problemSlug: "3sum", description: "Second gap." })]}
      />,
    )
    click(/i have this now/i)

    await waitFor(() => expect(text(container)).toContain("Second gap."))
    expect(text(container)).not.toContain(item().description)
  })

  it("empties itself once the last one is done, without waiting for a refetch", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome())

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() => expect(text(container)).toContain("Nothing due for review"))
    // The server has not been asked for a new list, and it does not need to be.
    expect(reviewMistake).toHaveBeenCalledTimes(1)
  })

  it("remembers where the last one left off once the queue is empty", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome({ intervalDays: 3 }))

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() => expect(text(container)).toContain("Back in 3 days."))
  })

  it("refreshes what the rest of the screen shows, because the award moved it", async () => {
    vi.mocked(reviewMistake).mockResolvedValue(outcome())

    render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() => expect(refreshMock).toHaveBeenCalled())
  })
})

describe("when the review does not go through", () => {
  it("keeps the card, so the learner can try again instead of losing it", async () => {
    vi.mocked(reviewMistake).mockRejectedValue(new Error("That mistake is not due yet."))

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() =>
      expect(screen.getByRole("alert").textContent).toContain("That mistake is not due yet."),
    )
    expect(text(container)).toContain(item().description)
    expect(text(container)).not.toContain("Nothing due for review")
  })

  it("explains itself when the failure has no message to give", async () => {
    vi.mocked(reviewMistake).mockRejectedValue("not an error object")

    render(<ReviewQueue items={[item()]} />)
    click(/still not/i)

    await waitFor(() =>
      expect(screen.getByRole("alert").textContent).toMatch(/did not go through/i),
    )
  })

  it("will not send a second answer while the first is still in flight", async () => {
    let release: (value: ReviewOutcome) => void = () => {}
    vi.mocked(reviewMistake).mockReturnValue(
      new Promise<ReviewOutcome>((resolve) => {
        release = resolve
      }),
    )

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() =>
      expect(
        (screen.getByRole("button", { name: /i have this now/i }) as HTMLButtonElement).disabled,
      ).toBe(true),
    )
    expect(
      (screen.getByRole("button", { name: /still not/i }) as HTMLButtonElement).disabled,
    ).toBe(true)

    release(outcome())
    await waitFor(() => expect(text(container)).toContain("Nothing due for review"))
  })

  it("does not award the card after a failed review, so it stays reviewable", async () => {
    vi.mocked(reviewMistake).mockRejectedValue(new Error("offline"))

    const { container } = render(<ReviewQueue items={[item()]} />)
    click(/i have this now/i)

    await waitFor(() => expect(screen.getByRole("alert")).toBeDefined())
    expect(reviewMistake).toHaveBeenCalledTimes(1)
    expect(text(container)).not.toContain("+20 XP")
    expect(refreshMock).not.toHaveBeenCalled()
  })
})