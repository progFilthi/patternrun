# PatternRun Frontend (Phase 2)

The first playable training slice: dashboard, pattern picker, problem list, and one complete
training loop for any seeded problem. It is a thin client over the Phase 1 read-only API. All
content, including every animation, comes from the backend.

Complete. `Phase 2` here means the release stage defined in README section 154, which is the only
sense the word has in this repository.

## Stack

Next.js 16 (App Router, React 19, Turbopack), TypeScript strict, Tailwind v4, shadcn/ui
primitives, Inter plus Geist Mono. No state library, no animation library: the training loop is
a small state machine in one component and motion is CSS only.

## Running

``` bash
# 1. database and API
docker compose up -d postgres
cd backend && ./mvnw spring-boot:run

# 2. frontend
cd frontend
npm install
npm run dev            # http://localhost:3000
```

| Variable | Used by | Default |
| --- | --- | --- |
| `API_URL` | server components | `http://localhost:8080` |
| `NEXT_PUBLIC_API_URL` | browser | `http://localhost:3000` pattern above, see `.env.example` |

Checks: `npm run build`, `npm run lint`, `npx tsc --noEmit`.

## Routes

| Route | Rendering | Purpose |
| --- | --- | --- |
| `/` | server | Dashboard: what to train now, pattern system, start-here problems |
| `/patterns` | server | Pattern picker in learning path order |
| `/patterns/[slug]` | server | Signal, mental model, recognition rules, template, invariant, problems |
| `/problems` | server | Problem library with pattern filter (`?pattern=`) |
| `/problems/[slug]` | server shell + client session | The training loop |

Every route is `force-dynamic`: content is never baked into a build, so a sleeping API cannot
serve stale or missing pages, and the free hosting tier can wake up per request.

## Training loop

`TrainingSession` (client) is the state machine. Phases are one at a time, no skipping:

``` text
SCOUT -> PATTERN_GUESS -> ANIMATION -> HINTS -> EXPLANATION -> COMPLEXITY -> COMPLETE
```

Rules that make it a training loop rather than a page:

- The pattern must be chosen and confirmed before the walkthrough unlocks. The correct answer is
  only revealed after committing.
- A `QUESTION` animation step blocks "Next" until it is answered, and shows the explanation
  afterwards. This is the predict-the-move mechanic.
- Hints reveal one rung at a time; the count is tracked as hint dependency.
- Complexity must be committed and checked before the session can finish.
- Finishing writes a completion record to `localStorage` (`patternrun.progress.v1`). No accounts,
  no XP maths, no server writes yet.

### Moving around inside a session

`SessionProgress` is a stepper, not a progress bar: every phase the learner has already reached
is a button that jumps straight back to it, so moving between Read, Identify, Animate, Hint and
Explain never means restarting the problem or going back through browser history.

The gate is deliberately one-directional. `furthestPhase` records the highest phase reached, and
only phases up to it are clickable; because the sole way forward is a Continue button that stays
disabled until its gate is met, "reached" and "earned" are the same thing. Going back never
discards state, so a learner can re-read the invariant after finishing, or jump back to the
animation to replay a prediction, without losing hints or answers.

The rail sticks below the header (`top-16`) so the way back is on screen at any scroll depth, and
scrolls horizontally on narrow screens. The `overflow-x` sits on the `nav`, not the inner `ol`:
the inner scroll container does not clip the document's scrollable overflow, which put a
horizontal scrollbar on every mobile problem page.

Two other pieces of orientation carry their own weight: `Breadcrumb` on a problem page
(`Problems > Pattern > Problem`) says which pattern a problem belongs to, and the Explanation
step is ordered as an argument rather than four equal paragraphs. The invariant is the claim and
gets the visual weight, the pseudocode is the machine that maintains it, `why it works` and
`what it beats` are the correctness argument and the rejected alternative, and `say it in one
sentence` turns it into three beats the learner can recite.

## AnimationRenderer

The centre of the frontend. Animation steps are data, so a registry maps a step type to a
component. There is no per-problem animation code anywhere.

``` text
AnimationStep (from the API)
   -> AnimationStage        step index, autoplay, speed, keyboard, progress rail
   -> AnimationRenderer     registry: AnimationStepType -> component
   -> renderers/*           one generic component per type
   -> payload.ts            safe readers for the free form JSON payload
   -> primitives.tsx        shared cells, pointers, stage frame
```

Rules the renderer follows:

- Payload access is always through `payload.ts` readers, which return fallbacks. A partial or
  unexpected payload renders a note instead of crashing the session.
- `text` on every step is rendered as a text alternative next to the visual (README section 56).
- No information is carried by colour alone: pointers, the window range and answers all have
  labels or text.
- Motion respects `prefers-reduced-motion`; autoplay is disabled when it is set.
- Keyboard: `←` previous, `→` next, `space` play/pause.

Types currently implemented: `ARRAY`, `POINTER`, `WINDOW`, `HASH_MAP`, `STACK`, `HEAP`, `TREE`,
`GRAPH`, `GRID`, `INTERVAL`, `PREFIX_SUM`, `DP_TABLE`, `CODE`, `TEXT`, `QUESTION`, `SUCCESS`,
`FAILURE`. All of them are exercised by the seeded content except `CODE`, which no seeded
problem uses yet.

### Payload contract consumed

``` text
ARRAY / POINTER   values, pointers {name: index}, highlight [index]
WINDOW            values, left, right, valid
HASH_MAP          entries [{key, value}], lookup {key, found}
STACK             items [], top index
HEAP              items [{...}], size
TREE              values, parents [-1 for root], highlight [index]
GRAPH             cells [[...]], visited [[r, c]], queue [[r, c]], current [r, c]
INTERVAL          intervals [[start, end]], highlight index, merged [[start, end]]
PREFIX_SUM        values, prefix, currentIndex
DP_TABLE          cells [[...]], rowLabels, colLabels, current [row, col]
CODE              language, code
TEXT              lines []
QUESTION          prompt, options [], answerIndex, explanation
SUCCESS/FAILURE   message, lesson (failure only)
```

Adding a type means: add the enum value on the backend, add one renderer file, register it in
`animation-renderer.tsx`, and extend the union in `types/api.ts`.

## Design

Restrained and tool-like on purpose: Inter, near-black on white, one accent, hairline borders,
no gradients, `8px` radius, spacing on a 4/8px scale, motion limited to 200–300ms colour
transitions. shadcn supplies `Button`, `Badge`, `Progress`, `Separator`, `RadioGroup`,
`Skeleton`; everything product specific (`PatternList`, `ProblemList`, `AnimationStage`,
`HintLadder`, `CompletionSummary`) is our own.

Hierarchy comes from three shared pieces in `components/layout/page-header.tsx`, so every route
reads at the same weight: `PageHeader` (mono eyebrow, title, lede, actions), `Section` (titled
band with a hairline and an optional trailing action), and the header nav, whose current route is
marked with `aria-current` plus an underline bar rather than colour alone.

### Dark mode

One token set drives both themes. `globals.css` defines the palette on `:root` and a `.dark`
block that swaps the surface ladder (`background` -> `card` -> `popover`), inverts `primary`, and
lightens the semantic colours so they stay legible on a dark background. Components only ever
reference tokens, so there are no per-component theme overrides to keep in sync.

Rules that make the switch work without a flash:

- The theme is chosen by `THEME_INIT_SCRIPT` (`lib/theme.ts`), an inline script in the root
  layout that sets the class on `<html>` before first paint. Reading the theme from React instead
  would flash the wrong theme, because the server can know neither localStorage nor the OS
  preference. `<html>` therefore carries `suppressHydrationWarning`.
- `ThemeToggle` picks its icon and label through the `dark:` CSS variant rather than React
  state, so the server markup and the first client render are identical and hydration cannot
  mismatch. The click handler reads the class the init script already applied.
- The choice is stored under `patternrun.theme.v1`. With no stored choice the app follows
  `prefers-color-scheme`, keeps following it if the OS setting changes, and syncs across tabs.
- `color-scheme` is set per theme so form controls, scrollbars and the canvas match.

## Not built yet

XP, levels, streaks, mastery, mistake journal, review queue, speedrun mode, the code editor and
any server-side execution, accounts, and server persisted attempts. All of that belongs to the
next phases; the loop above is the thing that has to work first.