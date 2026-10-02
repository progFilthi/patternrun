import { render, screen } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { getProgressSummary, getReviewQueue } from "@/lib/api/progress"
import type { DailyProgress, PatternMastery, ProgressSummary, ReviewItem } from "@/types/api"

vi.mock("@/lib/api/progress", () => ({ getProgressSummary: vi.fn(), getReviewQueue: vi.fn() }))
vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh: vi.fn() }) }))

/**
 * What the progress screen shows.
 *
 * The screen has no opinion of its own: every figure here arrives from the backend. So these tests
 * check two things and no others — that the numbers are rendered as given, and that the two places
 * a server value is easily misread are handled deliberately. A mastery axis the backend left
 * unmeasured is rendered as a dash rather than 0%, and an unreachable API renders as an admission
 * rather than a row of zeroes, because "you have done nothing" and "we could not ask" are different
 * claims and only one of them is true of a learner who has never trained.
 */

const today: DailyProgress = {
  day: "2026-01-01",
  xpEarned: 120,
  problemsCompleted: 2,
  patternsIdentified: 1,
  mistakesReviewed: 1,
  speedrunsCompleted: 0,
  activeMinutes: 14,
  goalMet: true,
  questCompleted: false,
  questItemsCompleted: 0,
}

const hashing: PatternMastery = {
  patternSlug: "hashing",
  patternName: "Hashing",
  difficultyOrder: 2,
  recognition: 80,
  correctness: 60,
  explanation: 40,
  overall: 62.5,
  attemptsCount: 3,
}

function summary(overrides: Partial<ProgressSummary> = {}): ProgressSummary {
  return {
    totalXp: 1240,
    level: 3,
    levelName: "Steady",
    levelProgress: 41.6,
    streak: 2,
    longestStreak: 5,
    trainedToday: true,
    today,
    patterns: [hashing],
    problemsCompleted: 7,
    openMistakes: 1,
    ...overrides,
  }
}

function dueItem(overrides: Partial<ReviewItem> = {}): ReviewItem {
  return {
    mistakeId: "m1",
    problemSlug: "two-sum",
    problemTitle: "Two Sum",
    category: "PATTERN",
    description: "Identified the pattern as sliding-window.",
    lesson: "Work from the constraints.",
    reviewCount: 0,
    intervalDays: 0,
    dueAt: "2026-01-01T00:00:00Z",
    attemptsSince: 1,
    ...overrides,
  }
}

async function renderPage() {
  const Page = (await import("@/app/progress/page")).default
  const element = await Page()
  return render(element)
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(getProgressSummary).mockResolvedValue(summary())
  vi.mocked(getReviewQueue).mockResolvedValue([])
})

describe("the figures the backend computed", () => {
  it("shows the level, XP and streak as given", async () => {
    await renderPage()

    expect(screen.getByText("Steady, level 3")).toBeDefined()
    expect(screen.getByText("1,240")).toBeDefined()
    expect(screen.getByText("2 days")).toBeDefined()
    expect(screen.getByText("7")).toBeDefined()
  })

  it("shows today's counters, including the goal", async () => {
    await renderPage()

    expect(screen.getByText("120")).toBeDefined()
    expect(screen.getByText("2")).toBeDefined()
    expect(screen.getByText("met")).toBeDefined()
  })

  it("gives today's section room to breathe, rather than a dash for an unused field", async () => {
    await renderPage()

    expect(screen.queryByText("—")).toBeNull()
  })

  it("omits today entirely when there is no today to show", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(summary({ today: undefined }))

    const { container } = await renderPage()

    expect(screen.getByText("Total XP")).toBeDefined()
    expect(container.textContent ?? "").not.toContain("Daily goal")
  })
})

describe("mastery axes", () => {
  it("rounds each axis to a whole percentage, as a learner reads it", async () => {
    await renderPage()

    expect(screen.getByText("80%")).toBeDefined()
    expect(screen.getByText("63%")).toBeDefined()
  })

  it("shows a dash for an axis the backend has not measured", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(
      summary({
        patterns: [{ ...hashing, correctness: undefined, overall: undefined }],
      }),
    )

    await renderPage()

    // Unmeasured is not zero, and rendering it as 0% would tell a learner they are bad at
    // something they have never attempted. Two axes were left out, so there are two dashes.
    expect(screen.getAllByText("—")).toHaveLength(2)
    expect(screen.getByText("80%")).toBeDefined()
  })

  it("does not show a zero-width bar as though the axis had been scored", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(summary({ patterns: [] }))

    const { container } = await renderPage()

    expect(container.textContent ?? "").toContain("Mastery appears once you have completed")
    expect(screen.queryByRole("meter")).toBeNull()
  })

  it("links a pattern to where it can be practised", async () => {
    await renderPage()

    const link = screen.getByText("Hashing")
    expect(link.getAttribute("href")).toBe("/patterns/hashing")
  })

  it("calls a pattern with no completed sessions not started, in plain words", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(
      summary({ patterns: [{ ...hashing, attemptsCount: 0 }] }),
    )

    const { container } = await renderPage()

    expect(container.textContent ?? "").toContain("not started")
  })
})

describe("the review queue on the screen", () => {
  it("hands the server's due list straight to the queue", async () => {
    vi.mocked(getReviewQueue).mockResolvedValue([dueItem()])

    const { container } = await renderPage()

    expect(container.textContent ?? "").toContain("Identified the pattern as sliding-window.")
  })

  it("reports how many mistakes are outstanding on the problems figure", async () => {
    await renderPage()

    expect(screen.getByText("1 to review")).toBeDefined()
  })

  it("says nothing about outstanding mistakes when there are none", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(summary({ openMistakes: 0 }))

    const { container } = await renderPage()

    expect(container.textContent ?? "").not.toContain("to review")
  })
})

describe("a learner who has not trained yet", () => {
  it("is not told they are level 1 of a level they have not reached", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(
      summary({ problemsCompleted: 0, patterns: [], today: undefined, totalXp: 0 }),
    )

    await renderPage()

    expect(screen.getByText("No sessions yet")).toBeDefined()
    expect(screen.getByText(/Finish one problem and this page starts answering/)).toBeDefined()
  })

  it("still offers the review queue, since an empty queue is not an error", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(summary({ problemsCompleted: 0 }))

    const { container } = await renderPage()

    expect(container.textContent ?? "").toContain("Nothing due for review")
  })
})

describe("when the backend cannot be reached", () => {
  it("admits it rather than rendering zeroes the learner would believe", async () => {
    vi.mocked(getProgressSummary).mockResolvedValue(null)

    const { container } = await renderPage()

    expect(container.textContent ?? "").toContain("The API is not reachable right now")
    expect(screen.queryByText("0")).toBeNull()
    expect(screen.queryByText("Total XP")).toBeNull()
  })
})