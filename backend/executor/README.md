# The executor image

A learner's Python runs here, in a throwaway container, and nowhere else.

The API never executes submitted code. It runs the `docker` CLI, which asks the daemon for a
container built from this image. Everything that makes that safe is a flag on that command line —
see `DockerExecutionProvider.command()` — and none of it lives in this directory. This directory's
only job is the part flags cannot provide: a Python with nothing in it worth stealing.

## Build

```sh
docker build -t patternrun-executor:py3 backend/executor
```

## Run

```sh
docker compose up -d postgres
PATTERN_RUNNER_ENABLED=true ./mvnw -f backend/pom.xml spring-boot:run
```

`PATTERN_RUNNER_ENABLED` defaults to `false`, and that default is the security model rather than a
convenience. Without a runner configured the API answers "running code is not enabled on this
server" and the editor says so. There is deliberately **no in-process fallback**: "the sandbox is
missing, so let us just run it here" is the one behaviour this feature must never have.

## What the image contains

`slim-bookworm` plus a Python and nothing else. No package manager, no shell utilities, no `curl`,
no compiler, no `git`, and the apt and pip caches are removed from the layers that used them.

It has no `ENTRYPOINT` and no `CMD`. The API supplies the interpreter invocation and the harness,
so a container started by hand cannot be coaxed into doing something else.

## What the container adds at run time

| Flag | What it stops |
|---|---|
| `--network=none` | Exfiltration, DNS, and calling home |
| `--read-only` + `--tmpfs /tmp` | Rewriting the image, and anywhere to stash something |
| `--user=65534:65534` | Running as root, or as the API's user |
| `--cap-drop=ALL`, `--security-opt=no-new-privileges` | Escalating from what it already has |
| `--memory=256m`, `--memory-swap=256m` | A runaway allocation taking the host with it |
| `--cpus=0.5` | A tight loop saturating the machine |
| `--pids-limit=32` | A fork bomb |
| `--rm` | Containers accumulating |
| *(no `-e`)* | The API's environment, including its database credentials, arriving |
| *(no bind mounts)* | The container learning a host path exists |

`DockerExecutionProviderIT` asserts each of these against a real container, and skips itself if the
image is not built.

## Changing the isolation

Read `DockerExecutionProvider.command()` as a whole before touching it. Dropping `--network=none`
or `--read-only` does not degrade this feature — it converts it from "runs a learner's program in a
box" into "runs a learner's program".

## Known limitations

- **Container start dominates the latency.** A Run or Submit takes roughly 0.3–1s, most of it the
  container starting. A long-lived pool would fix it and is the obvious next step; it is not built
  because nothing needs it yet and a pool is a resource-exhaustion problem of its own.
- **One language.** Python only. The provider interface is the seam, so a second language is a
  second implementation rather than a change to the training domain.
- **The harness is not the boundary.** It translates failures so a learner can act on them, and it
  guards stdout and redirects the interpreter's own streams, but anything that gets to run can
  still be clever inside the container. The container is what holds.
- **No seccomp or AppArmor profile.** Docker's defaults apply. A hardened profile is a reasonable
  hardening step and is not needed for the guarantees above to hold.