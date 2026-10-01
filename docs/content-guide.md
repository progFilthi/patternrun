# Problem Content Guide

Problem content is the product. This guide describes the contract for a single file in
`backend/src/main/resources/seed/problems/<slug>.json`.

The migration `V2__seed_content` rejects incomplete content, so a file that violates this
contract fails the build. `SeedContentIT` re-checks the acceptance criteria of README
section 92.

## Writing order

Follow README section 85. Write the file in this order, because the order is the teaching
order:

1. What are we asked to find? (`statement`, `examples`)
2. What makes brute force expensive? (`bruteForce`)
3. What signal suggests the pattern? (`primaryPattern`, `whyThisPattern`)
4. What state do we need? (`invariant`)
5. What changes each step? (`animationSteps`)
6. What is the template? (`pseudocode`)
7. What are the edge cases? (`constraints`, `commonMistakes`, `testCases`)
8. What is the complexity? (`timeComplexity`, `spaceComplexity`)
9. How would I explain this in an interview? (`interviewExplanation`)

## Required fields

| Field | Rule |
| --- | --- |
| `slug` | unique, kebab case, used in the URL |
| `externalId` | unique LeetCode number |
| `title`, `statement`, `difficulty`, `trainingDifficulty` | non empty |
| `primaryPattern` | slug of a seeded pattern, must exist |
| `secondaryPatterns` | may be empty, every entry must exist |
| `constraints` | at least 2 |
| `examples` | at least 1, each with `input`, `output`, `explanation` |
| `hints` | exactly the five ladder levels, each revealing one new idea |
| `pseudocode` | at least 4 lines |
| `timeComplexity`, `spaceComplexity` | canonical form such as `O(n)`, `O(log n)`, `O(n + m)` |
| `invariant` | the property that must stay true |
| `whyThisPattern` | answers "why this pattern and not brute force" |
| `bruteForce` | names the brute force complexity and a concrete operation count |
| `commonMistakes` | at least 2, each one sentence |
| `testCases` | at least 2, at least one `hidden: true` |
| `solutions` | at least one `JAVA` reference solution |
| `animationSteps` | at least 5, ordered, each with a `text` alternative, at least one `QUESTION` |

## Hints

Hint levels follow README section 7 and must escalate:

``` text
1 direction      what information would help
2 pattern        which pattern or idea applies
3 structure      which data structure or state
4 pseudocode     language free steps
5 implementation the concrete implementation
```

A hint that reveals the answer is a content bug. Never let level 1 name the data structure.

## Animation steps

Use the payload vocabulary in `docs/architecture.md`. Keep steps short and reversible, and let
one question step carry the decision the learner has to predict. The `text` field must describe
the visual state for someone who cannot see the animation.

## Difficulty

`difficulty` is the LeetCode difficulty. `trainingDifficulty` is the training ladder from
README section 30: `RECOGNITION`, `GUIDED`, `INDEPENDENT`, `SPEEDRUN`, `BOSS`.

A Medium problem can be `RECOGNITION` if the learner is new to the pattern.

## Adding a problem

1. Create the JSON file.
2. If the pattern does not exist yet, create its file under `seed/patterns/`.
3. Run `cd backend && ./mvnw verify`.

Existing seeded content is not re-applied to a database that already ran migration V2. For a
local reset:

``` bash
docker compose down -v && docker compose up -d postgres
```