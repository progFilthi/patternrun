# Local Development

## IntelliJ IDEA

The repository ships shared run configurations in `.run/`, so the Run button works with no
manual setup beyond opening the project once.

| Configuration | What it does |
| --- | --- |
| `PatternRun API` | Spring Boot app, `com.patternrun.PatternRunApplication`, working dir `backend/` |
| `PatternRun Web` | `npm run dev` in `frontend/` |
| `PatternRun Full Stack` | Compound: both of the above |

The repo root holds an aggregator `pom.xml` that lists `backend` as a module. IntelliJ detects a
pom in the project root on its own, so opening the project offers **Load Maven Project** with no
prior configuration. The backend module is named `patternrun-api` (after its `artifactId`), which
is what the run configurations point at.

If the offer does not appear, right click `backend/pom.xml` → **Add as Maven Project**.

What is already configured and why:

- The root `pom.xml` plus a root Maven wrapper (`./mvnw`) make the import automatic and let the
  whole build run from the repo root.
- `.idea/misc.xml` pins the project JDK to the installed Temurin 25 and the language level to 25,
  matching `maven.compiler.release`.
- `.idea/compiler.xml` enables annotation processing. Without it Lombok does not work in the
  editor, even though the Maven build is fine.
- `.run/PatternRun API.run.xml` sets `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and
  `CORS_ALLOWED_ORIGINS` so the app connects to the local database with no `.env` file.

There is no system `mvn` on this machine, so in `Settings → Build Tools → Maven` pick either
**Maven home: Bundled** or **Use Maven wrapper** (root `./mvnw`). Both work; the wrapper matches
what the command line and CI use.

### Do I have to restart IntelliJ?

Yes, once. IntelliJ had this project open with a stub module, so it is holding that model in
memory and will not notice new `.idea` files or the new root `pom.xml`. Fully quit IntelliJ
(`Cmd+Q`, not just close the project window) and reopen it, then accept **Load Maven Project**.

`File → Reload All from Disk` is enough for the run configurations to appear, but the Maven
import still needs the project reopened to be reliable.

## Command line

``` bash
# database (host port 5433, because 5432 is usually taken)
docker compose up -d

# API  -> http://localhost:8080
cd backend && ./mvnw spring-boot:run

# web  -> http://localhost:3000
cd frontend && npm install && npm run dev
```

``` bash
./mvnw clean verify                  # whole repo: builds backend, 16 unit + 21 integration tests
cd backend && ./mvnw clean verify    # backend only, same tests
cd frontend && npm run build && npm run lint && npx tsc --noEmit
```

## Stack versions

| Component | Version |
| --- | --- |
| Java | 25 (Temurin 25.0.4.1) |
| Spring Boot | 4.1.1 (Spring Framework 7.0.9, Hibernate 7.4.5) |
| Jackson | 3.x (`tools.jackson`, not `com.fasterxml`) |
| Flyway | 12.x |
| PostgreSQL | 18 (`postgres:18-alpine`, compose and Testcontainers) |
| Node | 25.x for the Next.js app |

Boot 4 notes that matter here: auto-configurations are separate modules
(`spring-boot-flyway`, `spring-boot-webmvc-test`), Hibernate 7 needs
`hibernate.type.json_format_mapper: jackson3`, and the PostgreSQL 18 image stores `PGDATA` in a
versioned subdirectory, so the compose volume mounts `/var/lib/postgresql` rather than
`/var/lib/postgresql/data`.

## Ports

| Port | Used by | Change with |
| --- | --- | --- |
| 5433 | local PostgreSQL 18 | `POSTGRES_PORT=5432 docker compose up -d` |
| 8080 | API | `PORT=9090 ./mvnw spring-boot:run` |
| 3000 | Next.js | `npm run dev -- --port 3001` |

If you change the database port, change `DB_URL` too (both defaults live in
`backend/src/main/resources/application.yml`).

## Content changes

`backend/src/main/resources/seed` is the single content source of truth. `ContentSeeder` skips
when content already exists, so after editing a content file reset the database (also required
after a PostgreSQL major version change):

``` bash
docker compose down -v && docker compose up -d
```

Then start the API again.