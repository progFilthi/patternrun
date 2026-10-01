# PatternRun Architecture

Phase 1 delivers the foundation and the content domain: the database, the domain model and a
read only API that the training UI will consume. The README is the product specification; this
document describes what exists today and how to run it.

## Stack

``` text
Browser (Next.js, not started yet)
   |  HTTPS
   v
Spring Boot API  (backend/, modular monolith)
   |
   v
PostgreSQL 17 (Flyway owns the schema)
```

One Spring Boot application. No microservices, no message broker, no code execution service.

## Modules

| Package | Responsibility |
| --- | --- |
| `com.patternrun.pattern` | Patterns: teaching content, template, invariant |
| `com.patternrun.problem` | Problems, examples, hint ladder, animation steps, test cases, solutions |
| `com.patternrun.common` | Error handling, pagination envelope |
| `com.patternrun.config` | CORS |
| `db.migration` | Flyway migrations, including the content seeder |

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

Product content is version controlled JSON under `backend/src/main/resources/seed`:

``` text
seed/patterns/*.json    10 patterns
seed/problems/*.json    20 problems, 2 per pattern
```

`V2__seed_content` loads them and validates on the way in: a missing field, an unknown pattern
slug or a blank hint fails the migration instead of shipping broken content.
`SeedContentIT` re-checks the acceptance criteria of README section 92 on every build.

## API

Base path `/api/v1`. All endpoints are read only in Phase 1.

``` http
GET /api/v1/patterns
GET /api/v1/patterns/{slug}
GET /api/v1/patterns/{slug}/problems

GET /api/v1/problems?pattern=&difficulty=&page=&size=&sort=
GET /api/v1/problems/{slug}
GET /api/v1/problems/{slug}/hints
GET /api/v1/problems/{slug}/animation
GET /api/v1/problems/{slug}/test-cases
```

Hidden test cases are never returned. The list endpoint is paginated and excludes hints,
animation and test cases so the dashboard never loads every animation for every problem
(README section 93).

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

If port 5432 is already taken: `POSTGRES_PORT=5433 docker compose up -d postgres` and start the
API with `DB_URL=jdbc:postgresql://localhost:5433/patternrun`.

``` bash
cd backend
./mvnw verify     # unit tests + integration tests on a throwaway Postgres container
```

## Configuration

| Variable | Default | Used in |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/patternrun` | local |
| `DB_USERNAME` / `DB_PASSWORD` | `patternrun` | local |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | local |
| `PORT` | `8080` | Render |

Production values come from environment variables only. Never commit secrets.

## Next phase

Phase 2 owns the training loop: attempts, hint ladder tracking, pattern guess, completion,
XP and mastery. That is where the `users` and progress tables come in, and it should not start
before the frontend consumes the contracts above.