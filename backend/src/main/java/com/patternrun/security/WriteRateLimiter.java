package com.patternrun.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * A fixed-window rate limiter for the write endpoints (README section 90).
 *
 * In-process and deliberately so. There is one API instance and no shared cache to coordinate
 * with, and the thing being protected is an XP ledger whose idempotency is already enforced by a
 * unique index. A distributed limiter would add infrastructure to guard a table that cannot be
 * double-credited anyway. If the API ever runs as more than one instance this needs replacing,
 * because each instance would then count its own requests.
 *
 * The window is fixed rather than sliding: a leaky bucket needs per-key timestamps and this
 * needs one integer per key. The cost is a learner can spend their budget at the end of one
 * window and the start of the next, which is irrelevant at this threshold.
 */
@Component
public class WriteRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final Duration window;
    private final Clock clock;

    public WriteRateLimiter() {
        this(120, Duration.ofMinutes(1), Clock.systemUTC());
    }

    WriteRateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /**
     * Records a write against a caller, or throws when they are over budget.
     *
     * @param caller a learner id, or a client address for a request with no session
     */
    public void check(String caller) {
        Instant now = clock.instant();
        Window updated = windows.compute(caller, (key, existing) -> {
            if (existing == null || existing.startedAt.plus(window).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(existing.startedAt, existing.count + 1);
        });

        if (updated.count > limit) {
            throw new RateLimitExceededException(
                    "Too many requests. Give it a minute and try again.");
        }
    }

    /** Drops windows that have expired, so the map cannot grow without bound. */
    public void evictExpired() {
        Instant cutoff = clock.instant().minus(window);
        windows.entrySet().removeIf(entry -> entry.getValue().startedAt.isBefore(cutoff));
    }

    private record Window(Instant startedAt, int count) {
    }
}