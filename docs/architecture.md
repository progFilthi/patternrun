# PatternRun Architecture

Phase 1 delivers the foundation and the content domain: the database, the domain model and a
read only API that the training UI will consume. The README is the product specification; this
document describes what exists today and how to run it.

## Stack

``` text
Browser (Next.js)
   |  HTTPS
   v
Spring Boot API  (backend/, modular monolith)
   |
   v
PostgreSQL 18 (Flyway owns the schema)
```

One Spring Boot application (4.1.1 on Java 25, Spring Framework 7, Hibernate 7). No
microservices, no message broker, no code execution service.

## Modules

| Package | Responsibility |
| --- | --- |
| `com.patternrun.pattern` | Patterns: teaching content, template, invariant |
| `com.patternrun.problem` | Problems, examples, hint ladder, animation steps, test cases, solutions |
| `com.patternrun.content` | Content seeding: validated reader plus idempotent Spring seeder |
| `com.patternrun.common` | Error handling, pagination envelope |
| `com.patternrun.config` | CORS |

Every layer follows `Controller -> Service -> Repository`. Controllers never return JPA
entities, they return records. Entities are never exposed in responses.

## Database

Flyway owns the schema; Hibernate runs with `ddl-auto: validate`, so a mismatch between entity
and migration fails startup instead of silently drifting.

``` text
patterns
problems
problem_secondary_patterns
problem_examples
problem_hints
problem_animation_steps   -- payload is JSONB
problem_test_cases
problem_solutions
```

`users`, `user_problem_attempts`, `user_pattern_mastery`, `user_daily_progress` and
`user_mistakes` are deliberately absent in Phase 1. For the MVP progress lives in the browser
(README sections 73 and 74); those tables arrive with the progress phase.

List valued content (constraints, pseudocode, recognition rules, common mistakes, animation
payloads) is stored as JSONB and mapped with Hibernate's JSON type.

## Content

Flyway owns the schema only. Product content is version controlled JSON under
`backend/src/main/resources/seed`, which is the single source of truth:

``` text
seed/patterns/*.json    10 patterns
seed/problems/*.json    20 problems, 2 per pattern
```

Loading is a normal Spring component, not a migration:

``` text
SeedContentReader   reads the JSON, maps it onto records, validates it
ContentSeeder       ApplicationRunner, idempotent, writes the validated content
```

`SeedContentReader` applies Bean Validation to the records plus the cross field rules only the
whole file can satisfy: unique slugs and problem numbers, known pattern references, a complete
five level hint ladder, at least one hidden test case, a Java reference solution, and at least
one `QUESTION` animation step. Invalid content fails startup with a precise message instead of
reaching the database.

`ContentSeeder` skips when the content tables already hold rows, so restarting the API never
duplicates content. It is a loader, not an editor: to apply a content change to an existing
database, reset it with `docker compose down -v`. Set `PATTERN_SEED_ENABLED=false` to turn the
pass off.

`SeedContentIT` re-checks the acceptance criteria of README section 92 against the database on
every build, and `SeedContentReaderTest` checks the files themselves.

## API

Base path `/api/v1`.

``` http
GET    /api/v1/patterns
GET    /api/v1/patterns/{slug}
GET    /api/v1/patterns/{slug}/problems

GET    /api/v1/problems?pattern=&difficulty=&page=&size=&sort=
GET    /api/v1/problems/{slug}
GET    /api/v1/problems/{slug}/breakdown
GET    /api/v1/problems/{slug}/hints
GET    /api/v1/problems/{slug}/animation
GET    /api/v1/problems/{slug}/test-cases

POST   /api/v1/auth/session
GET    /api/v1/auth/me
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout

POST   /api/v1/attempts
POST   /api/v1/attempts/{id}/pattern
POST   /api/v1/attempts/{id}/hint
GET    /api/v1/attempts/{id}/hint/next?stage=
POST   /api/v1/attempts/{id}/predict
POST   /api/v1/attempts/{id}/breakdown
POST   /api/v1/attempts/{id}/code
POST   /api/v1/attempts/{id}/code/save
GET    /api/v1/attempts/{id}/code
POST   /api/v1/attempts/{id}/code/run
POST   /api/v1/attempts/{id}/code/submit
POST   /api/v1/attempts/{id}/solution
POST   /api/v1/attempts/{id}/complete

GET    /api/v1/progress
GET    /api/v1/progress/patterns
GET    /api/v1/progress/problems
GET    /api/v1/progress/streak
GET    /api/v1/progress/daily
GET    /api/v1/progress/review
```

The coding-stage endpoints hang off the attempt rather than getting their own controller, because a
code submission is an event inside a training session, not a separate resource.

`/solution` is a POST on purpose. A `GET` would hand the answer to anyone who guessed the URL and
would leave no record that it happened; the POST against a live attempt means the fetch is itself
the recorded event.

### The client/server boundary

The browser sends what the learner chose and the source they wrote. It never sends a verdict.

``` text
sent      language, code, chosenIndex, level, stage, problemSlug,
          complexityTime, complexitySpace, durationMs, predictions

never     passed, accepted, outcome, correct, xp, awards, combo, mastery,
          grade, patternCorrect, complexityCorrect, breakdownCorrect,
          predictionsCorrect, codeAccepted, solvedIndependently,
          expected, entrypoint, trigger
```

`ExecuteCodeRequest` has no field a verdict could arrive in, so one is discarded rather than
honoured. `CodeExecutionApiIT` posts all of them and asserts the run still comes back
`WRONG_ANSWER`; `use-training-attempt.test.ts` asserts the same from the client side.

Two consequences worth remembering: the entrypoint is the server's, because a client that chose
which function to call could choose its own argument shape; and the hint trigger is the server's,
because a client that could name its own trigger could ask for the debugging ladder after a passing
submission.

Query parameters are validated with Bean Validation: `page >= 0`, `1 <= size <= 100`, `pattern`
must be a slug, `sort` must be a field name. Hidden test cases are never returned by any
endpoint. The list endpoint is paginated and excludes hints, animation and test cases so the
dashboard never loads every animation for every problem (README section 93).

Errors are uniform and never leak internals:

``` json
{
  "status": 404,
  "error": "Not Found",
  "message": "Problem not found: nope",
  "path": "/api/v1/problems/nope",
  "timestamp": "2026-10-01T15:38:27.545492Z"
}
```

## Animation payload vocabulary

Animations are data, never components (README section 28). Each step is
`{ order, type, title, description, text, payload }`. `text` is the accessible description
required by README section 56.

| type | payload |
| --- | --- |
| `ARRAY` | `values`, `pointers` (name to index), `highlight` |
| `POINTER` | `values`, `pointers`, `highlight` |
| `WINDOW` | `values`, `left`, `right`, `valid` |
| `HASH_MAP` | `entries` (`{key, value}`), `lookup` (`{key, found}` or null) |
| `STACK` | `items`, `top` |
| `HEAP` | `items`, `size` |
| `TREE` | `values`, `parents` (index to parent index, -1 for the root), `highlight` |
| `GRAPH` | `cells`, `visited`, `queue`, `current` |
| `INTERVAL` | `intervals`, `highlight`, `merged` |
| `PREFIX_SUM` | `values`, `prefix`, `currentIndex` |
| `DP_TABLE` | `rowLabels`, `colLabels`, `cells`, `current` |
| `CODE` | `language`, `code` |
| `TEXT` | `lines` |
| `QUESTION` | `prompt`, `options`, `answerIndex`, `explanation` |
| `SUCCESS` | `message` |
| `FAILURE` | `message`, `lesson` |

Every problem contains at least one `QUESTION` step, which is the predict-the-move mechanic
from README section 8.

## Running locally

``` bash
docker compose up -d postgres
cd backend
./mvnw spring-boot:run
```

With code execution, which needs the runner image built first:

``` bash
docker build -t patternrun-executor:py3 backend/executor
PATTERN_RUNNER_ENABLED=true ./mvnw spring-boot:run
```

`PATTERN_RUNNER_ENABLED` defaults to `false` and that default is the security model, not a
convenience. Without a runner the API reports "running code is not enabled on this server" and
there is no in-process fallback. See `backend/executor/README.md`.

The repo root also has an aggregator `pom.xml`, so `./mvnw verify` from the root builds the whole
project.

The local database listens on host port 5433 by default because 5432 is commonly taken. Override
either side when needed: `POSTGRES_PORT=5432 docker compose up -d postgres` together with
`DB_URL=jdbc:postgresql://localhost:5432/patternrun`.

``` bash
cd backend
./mvnw verify     # unit tests + integration tests on a throwaway Postgres container
```

## Configuration

| Variable | Default | Used in |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5433/patternrun` | local |
| `DB_USERNAME` / `DB_PASSWORD` | `patternrun` | local |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | local |
| `PATTERN_SEED_ENABLED` | `true` | both |
| `PATTERN_RUNNER_ENABLED` | `false` | local, with code execution |
| `PATTERN_RUNNER_IMAGE` | `patternrun-executor:py3` | local, with code execution |
| `PORT` | `8080` | Render |

Production values come from environment variables only. Never commit secrets.

## Runnable content

All twenty problems are runnable. Each carries an `entrypoint`, structured `call` and `expected` on
every test case, and a Python reference solution.

`problems.argument_mode` says how the runner turns stored arguments into a call. `PLAIN` is the
default and is the eighteen problems whose arguments are already the values their function wants.
`TREE` builds a binary tree from a level-order array for the two tree problems, so a learner's file
contains only their function.

`ReferenceSolutionsIT` runs every shipped reference against every shipped case, and is the reason
nine wrong published answers were caught. It skips without a Python interpreter.

## Deliberately not built yet

- **A second language.** Python only, by request. `CodeExecutionProvider` is the seam.
- **A container pool.** Each submission starts a container, which dominates latency. See
  `backend/executor/README.md`.
- **Staged hint rungs outside Two Sum.** Only Two Sum has `CODING`-stage hints. The other nineteen
  fall back to the generic five-rung ladder, which works but is less useful at the point of
  failure.
- **A second language.** Python only, by request. `CodeExecutionProvider` is the seam.
- **Gating Phase 3's awards on accepted code.** `codeAccepted` is reported and feeds nothing yet.
  That is a product decision to be made with the evidence visible.
- **Mistake-journal entries for code failures.** The execution log holds everything an entry would
  need; classifying them wants real data first.

## Next

Phase 4 proved the loop on Two Sum alone. Generalising the execution content to the remaining
nineteen problems is content work, not code work: the runner, the provider boundary and the editor
are already generic.