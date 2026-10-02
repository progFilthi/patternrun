package com.patternrun.progress.dto;

/**
 * Per-problem state for the library list.
 *
 * Keyed by slug and fetched separately from the content endpoints rather than embedded in them,
 * so an anonymous content request stays user independent and cacheable. One call, no N+1.
 */
public record ProblemProgressResponse(
        String problemSlug,
        int attempts,
        boolean completed,
        String lastGrade,
        Long bestDurationMs,
        int timesCompleted) {
}
