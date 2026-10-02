import { fireEvent, render, screen, within } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"

import { SessionProgress } from "@/components/training/session-progress"
import { FIRST_PHASE, LAST_PHASE, PHASES } from "@/lib/training/phases"

function renderRail({
  currentIndex = FIRST_PHASE,
  reachableIndex = FIRST_PHASE,
  onSelect = () => {},
}: {
  currentIndex?: number
  reachableIndex?: number
  onSelect?: (index: number) => void
} = {}) {
  render(
    <SessionProgress
      phases={PHASES}
      currentIndex={currentIndex}
      reachableIndex={reachableIndex}
      onSelect={onSelect}
    />,
  )
  return within(screen.getByRole("navigation", { name: "Training phases" }))
}

/** The seven buttons, in order. */
function stepButtons() {
  return screen.getAllByRole("button")
}

function reachableFlags() {
  return stepButtons().map((button) => !button.hasAttribute("disabled"))
}

describe("SessionProgress", () => {
  it("renders one control per phase, in loop order", () => {
    renderRail()

    expect(stepButtons()).toHaveLength(PHASES.length)
    expect(stepButtons().map((button) => button.textContent)).toEqual([
      expect.stringContaining("Read"),
      expect.stringContaining("Identify"),
      expect.stringContaining("Animate"),
      expect.stringContaining("Hint"),
      expect.stringContaining("Explain"),
      expect.stringContaining("Complexity"),
      expect.stringContaining("Code"),
      expect.stringContaining("Finish"),
    ])
  })

  it("is exposed as the training phases navigation landmark", () => {
    renderRail()
    expect(screen.getByRole("navigation", { name: "Training phases" })).toBeDefined()
  })

  describe("locking", () => {
    it("locks every phase past the reachable one", () => {
      renderRail({ currentIndex: 0, reachableIndex: 0 })
      expect(reachableFlags()).toEqual([true, false, false, false, false, false, false, false])
    })

    it("offers every phase up to the furthest reached", () => {
      renderRail({ currentIndex: 2, reachableIndex: 4 })
      expect(reachableFlags()).toEqual([true, true, true, true, true, false, false, false])
    })

    it("keeps a phase that was reached reachable after moving back to the start", () => {
      // The regression that matters: rereading Read must not re-lock what was already earned.
      renderRail({ currentIndex: FIRST_PHASE, reachableIndex: 4 })
      expect(reachableFlags()).toEqual([true, true, true, true, true, false, false, false])
      expect(stepButtons()[4].hasAttribute("disabled")).toBe(false)
    })

    it("opens everything once the last phase is reached", () => {
      renderRail({ currentIndex: LAST_PHASE, reachableIndex: LAST_PHASE })
      expect(reachableFlags().every(Boolean)).toBe(true)
    })
  })

  describe("moving between phases", () => {
    it("reports the index of a phase that was clicked", () => {
      const onSelect = vi.fn()
      renderRail({ currentIndex: 3, reachableIndex: 3, onSelect })

      fireEvent.click(stepButtons()[0])

      expect(onSelect).toHaveBeenCalledTimes(1)
      expect(onSelect).toHaveBeenCalledWith(0)
    })

    it("reports the exact index for every reachable phase", () => {
      const onSelect = vi.fn()
      renderRail({ currentIndex: 3, reachableIndex: 3, onSelect })

      for (const index of [0, 1, 2, 3]) {
        fireEvent.click(stepButtons()[index])
      }

      expect(onSelect.mock.calls.map(([index]) => index)).toEqual([0, 1, 2, 3])
    })

    it("lets the learner jump back from a late phase to the first one", () => {
      const onSelect = vi.fn()
      renderRail({ currentIndex: 4, reachableIndex: 4, onSelect })

      fireEvent.click(stepButtons()[FIRST_PHASE])

      expect(onSelect).toHaveBeenCalledWith(FIRST_PHASE)
    })

    it("reports a click on the current phase without complaint", () => {
      const onSelect = vi.fn()
      renderRail({ currentIndex: 2, reachableIndex: 4, onSelect })

      fireEvent.click(stepButtons()[2])

      expect(onSelect).toHaveBeenCalledWith(2)
    })

    it("never reports a click on a locked phase", () => {
      const onSelect = vi.fn()
      renderRail({ currentIndex: 1, reachableIndex: 1, onSelect })

      for (const button of stepButtons().slice(2)) {
        fireEvent.click(button)
      }

      expect(onSelect).not.toHaveBeenCalled()
    })
  })

  describe("marking the current phase", () => {
    it("marks exactly one phase as the current step", () => {
      renderRail({ currentIndex: 3, reachableIndex: 3 })

      const current = stepButtons().filter(
        (button) => button.getAttribute("aria-current") === "step",
      )

      expect(current).toHaveLength(1)
      expect(current[0].textContent).toContain("Hint")
    })

    it("marks the first phase as current when the session starts", () => {
      // currentIndex 0 is a real state, not "nothing is current": the learner is on Read.
      renderRail({ currentIndex: FIRST_PHASE, reachableIndex: FIRST_PHASE })

      const current = stepButtons().filter(
        (button) => button.getAttribute("aria-current") === "step",
      )

      expect(current).toHaveLength(1)
      expect(current[0].textContent).toContain("Read")
    })

    it("marks exactly one phase wherever the learner stands in the loop", () => {
      for (let index = FIRST_PHASE; index <= LAST_PHASE; index++) {
        const { unmount } = render(
          <SessionProgress
            phases={PHASES}
            currentIndex={index}
            reachableIndex={LAST_PHASE}
            onSelect={() => {}}
          />,
        )

        const current = screen
          .getAllByRole("button")
          .filter((button) => button.getAttribute("aria-current") === "step")

        expect(current).toHaveLength(1)
        expect(current[0].textContent).toContain(PHASES[index].label)
        unmount()
      }
    })

    it("does not rely on colour alone to mark the current phase", () => {
      // README section 56: state has to survive without colour.
      renderRail({ currentIndex: 2, reachableIndex: 2 })

      expect(stepButtons()[2].getAttribute("aria-current")).toBe("step")
      expect(stepButtons()[0].getAttribute("aria-current")).toBeNull()
      expect(stepButtons()[5].getAttribute("aria-current")).toBeNull()
    })
  })

  describe("accessible naming", () => {
    it("names a reachable phase with its position in the loop", () => {
      renderRail({ currentIndex: 0, reachableIndex: 3 })

      expect(stepButtons()[2].textContent).toContain("Go to step 3, Animate")
      expect(stepButtons()[3].textContent).toContain("Go to step 4, Hint")
    })

    it("says a locked phase is locked", () => {
      renderRail({ currentIndex: 0, reachableIndex: 0 })

      expect(stepButtons()[1].textContent).toContain("Locked: Identify")
      // By name rather than by index, so a future insertion cannot quietly change which phase
      // this is asserting about.
      const finishIndex = PHASES.findIndex((phase) => phase.id === "COMPLETE")
      expect(stepButtons()[finishIndex].textContent).toContain("Locked: Finish")
    })

    it("describes the lock in the tooltip too", () => {
      renderRail({ currentIndex: 0, reachableIndex: 0 })

      expect(stepButtons()[0].getAttribute("title")).toBe("Read")
      expect(stepButtons()[1].getAttribute("title")).toBe("Identify (locked)")
    })

    it("gives every phase a distinct accessible position", () => {
      renderRail({ currentIndex: 0, reachableIndex: LAST_PHASE })

      const announced = stepButtons().map((button) =>
        (button.textContent ?? "").replace(/\s+/g, " ").trim(),
      )

      expect(new Set(announced).size).toBe(PHASES.length)
    })
  })

  describe("progress marks", () => {
    it("numbers the current phase and the ones still ahead", () => {
      renderRail({ currentIndex: 1, reachableIndex: 1 })

      // Index 1 is where the learner stands, index 2 has not been passed.
      expect(stepButtons()[1].textContent).toContain("02")
      expect(stepButtons()[2].textContent).toContain("03")
      expect(stepButtons()[6].textContent).toContain("07")
    })

    it("does not keep the ordinal on a phase the learner has passed", () => {
      renderRail({ currentIndex: 1, reachableIndex: 1 })

      // Index 0 is behind the learner, so it carries a tick instead of "01".
      expect(stepButtons()[0].textContent).not.toContain("01")
      expect(stepButtons()[0].textContent).toContain("Read")
    })

    it("replaces the number with a tick once a phase is behind the learner", () => {
      renderRail({ currentIndex: 3, reachableIndex: 3 })

      // A completed phase no longer shows its ordinal, so the tick is unambiguous.
      expect(stepButtons()[0].textContent).not.toContain("01")
      expect(stepButtons()[0].textContent).toContain("Read")
      expect(stepButtons()[3].textContent).toContain("04")
    })

    it("ticks every phase behind the learner", () => {
      const { container } = render(
        <SessionProgress
          phases={PHASES}
          currentIndex={4}
          reachableIndex={4}
          onSelect={() => {}}
        />,
      )

      expect(container.querySelectorAll("svg")).toHaveLength(4)
    })
  })
})