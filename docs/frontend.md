# PatternRun Frontend (Phase 2)

The first playable training slice: dashboard, pattern picker, problem list, and one complete
training loop for any seeded problem. It is a thin client over the Phase 1 read-only API. All
content, including every animation, comes from the backend.

Phase 2 complete. `Phase 2` here means the release stage defined in README section 154, which is
the only sense the word has in this repository.

## Progress persistence

Phase 2 stored completions in `localStorage`. Phase 3 made the backend the record, and nothing
writes that key any more. It is still read, because the blob in a returning learner's browser
predates their account and importing it is how that history reaches the server.

**The browser only ever reports choices.** Every request carries what the learner picked or did:
a pattern slug, a hint level, a chosen option index, a complexity string. No request carries XP,
an award, a combo, a mastery score, or a claim that an answer was right. `CompletionResult`
arrives as a set of facts and is rendered as-is, which is why there is no XP arithmetic anywhere
in the frontend and nothing that could drift from the ledger.

`AnimationStage.onPrediction` deliberately takes `(stepOrder, optionIndex)` and not a correctness
flag. The answer is in the step payload the browser already has, so passing a verdict upwards
would make it a value a caller could send.

## Same origin

The API is proxied through this origin by a `next.config.ts` rewrite rather than called from the
browser directly. A cookie is only attached to same-site requests, and the frontend and API are on
different ports in development, which browsers treat as different sites. Calling cross-origin
would force `SameSite=None`, which forces a CSRF token and a credentialed CORS setup. Proxying
leaves the cookie `SameSite=Lax`, so CSRF protection is the framework default, and development
matches production.

## Tests

Vitest with React Testing Library and jsdom. `npm test` runs once, `npm run test:watch` watches.
Test files sit beside the code they cover as `*.test.ts` / `*.test.tsx`, and `tsconfig.json`
includes them, so the tests are type-checked by `npm run typecheck` like everything else.

## Stack

Next.js 16 (App Router, React 19, Turbopack), TypeScript strict, Tailwind v4, shadcn/ui
primitives, Inter plus Geist Mono. No state library, no animation library: the training loop is
a small state machine in one component and motion is CSS only.

CodeMirror 6 for the code editor, loaded behind `next/dynamic` with `ssr: false` because it touches
`document` on mount. Monaco was the alternative and would have been roughly 3-5MB for intellisense
this product does not want: the hint, the diagnosis and the explanation are the teaching surface.

## Running

``` bash
# 1. database and API
docker compose up -d postgres
cd backend && ./mvnw spring-boot:run

# 2. the code executor image, once. Only needed for Run and Submit.
docker build -t patternrun-executor:py3 backend/executor

# ...and the API must be started with the runner enabled, which is off by default
PATTERN_RUNNER_ENABLED=true ./mvnw spring-boot:run

# 3. frontend
cd frontend
npm install
npm run dev            # http://localhost:3000
```

Without `PATTERN_RUNNER_ENABLED=true` the API starts fine and the editor reports that the runner is
unavailable. That is the intended behaviour and not a misconfiguration to route around: there is
deliberately no in-process fallback, because "the sandbox is missing, so let us just run it here"
is the one behaviour this must never have.

| Variable | Used by | Default |
| --- | --- | --- |
| `API_INTERNAL_URL` | the `/api/v1` rewrite, and server reads | `http://localhost:8080` |
| `API_URL` | fallback for server reads | `http://localhost:8080` |
| `NEXT_PUBLIC_API_URL` | fallback only; browser calls are same-origin | see `.env.example` |

The browser does not use `NEXT_PUBLIC_API_URL` to reach the API. Phase 3 writes go through this
origin via the rewrite, so the session cookie is same-site and travels without any
credentialed-CORS arrangement. Set `API_INTERNAL_URL` to the API's reachable address.

Checks: `npm run build`, `npm run lint`, `npm run typecheck`, `npm test`.

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
SCOUT -> PATTERN_GUESS -> ANIMATION -> HINTS -> EXPLANATION -> COMPLEXITY -> CODE -> COMPLETE
```

`CODE` was inserted between `COMPLEXITY` and `COMPLETE`. Complexity is the last thing reasoned
about before writing, so the editor follows it directly. Its continue button used to say "Finish
the session" and it did; it now says "Write it", because a button that promises to finish the
session while opening a blank editor is a small lie.

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

The rules that decide what the rail may offer are pure functions in
`lib/training/phases.ts`, not inline in the component: `clampPhaseIndex`, `isReachable` and
`reachThrough`. Keeping them out of the component is what makes the gate testable, and the gate is
the part worth testing, because its failure mode is silent.

Two other pieces of orientation carry their own weight: `Breadcrumb` on a problem page
(`Problems > Pattern > Problem`) says which pattern a problem belongs to, and the Explanation
step is ordered as an argument rather than four equal paragraphs. The invariant is the claim and
gets the visual weight, the pseudocode is the machine that maintains it, `why it works` and
`what it beats` are the correctness argument and the rejected alternative, and `say it in one
sentence` turns it into three beats the learner can recite.

## Tests

Vitest with React Testing Library and jsdom. `npm test` runs once, `npm run test:watch` watches.
Test files sit beside the code they cover as `*.test.ts` / `*.test.tsx`, and `tsconfig.json`
includes them, so the tests are type-checked by `npx tsc --noEmit` like everything else.

``` text
phases.test.ts                    the loop, the clamp, reachability, monotonicity
session-progress.test.tsx         rendering, locking, onSelect, aria-current, accessible names
theme.test.ts                     the class, the stored choice, the OS fallback, storage failures
complexity.test.ts                folding O(n²) and O(n^2) to one answer
use-training-attempt.test.ts      the wire contract, the phase machine, every failure path
completion-summary.test.tsx       rendering the backend's verdict and nothing else
```

The two Phase 3 files carry the rules that matter most. `use-training-attempt.test.ts` asserts on
request bodies rather than on rendered output, because the security property is about what leaves
the browser: a change that helpfully started sending `patternCorrect`, or a locally computed total,
fails there. `completion-summary.test.tsx` asserts that a repeat reads as "No new XP" with an
explanation rather than as zero, and that a failure shows a reason and never a number.

Two environment notes, both in `src/test-setup.ts`:

- `@vitejs/plugin-react` is not used. It only adds Fast Refresh, which tests do not want, and its
  current release pulls a Babel toolchain that conflicts with the shadcn dependency tree. Vitest
  transforms TSX through esbuild, which reads `"jsx": "react-jsx"` from `tsconfig.json`.
- `localStorage` is polyfilled per file. Node 25 ships a built-in `localStorage` that needs
  `--localstorage-file`, and Vitest copies it over the working one jsdom provides, leaving an
  empty object. Without the polyfill any preference-reading component fails for a reason that has
  nothing to do with the component.

React Testing Library only registers its automatic cleanup when Vitest globals are enabled. This
suite imports `describe`/`it`/`expect` explicitly, so `cleanup()` is called from an `afterEach` in
the setup file instead. Without it every `render` appends to the same document and single-element
queries fail for the wrong reason.

### What is not covered

Vitest cannot render async Server Components, so the data-fetching routes are not unit tested and
stay covered by walking the running app. Testing them properly is an end-to-end suite, which is
the natural companion to the tests above once the routes start changing often.

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

## The editor

``` text
components/editor/code-editor.tsx          dynamic, ssr: false, theme from the document
components/editor/code-editor-surface.tsx  CodeMirror, loaded only where an editor renders
```

The public component takes `language`, `value`, `onChange` and `label`. It knows about syntax,
theming and Tab, and nothing about problems, attempts, running or scoring --- which is what lets the
same component serve the learner's solution, the revealed reference and a future Java editor without
any of them growing a special case. `EditorLanguage` already includes `plaintext`, so a language
without a grammar still renders a usable editor.

Two details worth knowing. Tab indents rather than leaving the field, and the editor stops the
animation stage's arrow-key paging from firing while someone is typing inside a string literal
--- otherwise typing an arrow in Python scrolls the walkthrough.

## Not built yet

The mistake journal and speedrun mode exist on the backend and have no UI. Code failures are not yet
written into the mistake journal, though `user_code_executions` holds everything such an entry
would need. Only Two Sum has an editor; the other nineteen have no structured test arguments, so
`runnableEntrypoint` is absent and the page says so rather than offering an editor that cannot work.

## What the coding screen must not become

The editor communicates correctness, progress, mistakes and improvement. It does not pay out. No XP
counter in the panel, no level-up, no confetti, no arcade treatment. A passing Run is labelled as a
pass on the *visible examples only* and followed by an instruction to submit, because a learner who
cannot tell those apart will stop before the thing that actually decides.

The one place the code result is mentioned again afterwards is a single quiet line on the completion
screen. The rewards above it are the celebration; adding a second one is the arcade UI this product
is not.