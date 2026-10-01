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
| `PORT` | `8080` | Render |

Production values come from environment variables only. Never commit secrets.

## Deliberately not built yet

Authentication, users, attempts, XP, mastery, code execution, AI and gamification are all out of
scope for Phase 1. Solutions are stored as content for the hint level 5 reveal and are not
served by any endpoint yet.

## Next phase

Phase 2 owns the training loop: attempts, hint ladder tracking, pattern guess, completion,
XP and mastery. That is where the `users` and progress tables come in, and it should not start
before the frontend consumes the contracts above.