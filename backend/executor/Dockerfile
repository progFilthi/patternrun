# The image a learner's Python runs in.
#
# The API never executes submitted code itself. It starts one of these per run, and the guarantees
# that make that safe are container flags rather than anything in this file: no network, a read-only
# root filesystem, a non-root user, no capabilities, a memory cap and a pid cap.
#
# What this file is responsible for is the part the flags cannot give us: a Python with nothing in
# it worth stealing. No package manager, no shell utilities, no curl, no compiler, no git.

FROM python:3.13-slim-bookworm

# Nothing here survives into the final image; they exist only to clear the caches pip leaves in
# the layers it used to populate.
RUN set -eux; \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*; \
    find /usr/local -name '__pycache__' -type d -prune -exec rm -rf {} +

# Belt and braces alongside the --read-only flag. If the read-only root is ever lifted, a Python
# that cannot write bytecode is a Python with nowhere to stage anything.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONHASHSEED=0

# Drop to nobody explicitly. The API also passes --user, and having both means the image is
# already non-root for anyone who runs it directly while debugging.
USER 65534:65534

# There is no ENTRYPOINT and no CMD on purpose. The API supplies the interpreter invocation and the
# harness, so a container started by hand without one cannot be coaxed into doing anything else.

# Sanity check at build time. An image whose Python cannot start is worse than no image, because
# the failure would only surface as a runner error for one learner at a time.
RUN [ "python3", "-I", "-B", "-c", "print('ok')" ]