package com.patternrun.progress.dto;

/**
 * A learner's judgement about one mistake they are reviewing.
 *
 * One boolean, and it is deliberately the only field. It is a claim about their own
 * understanding, which the backend cannot verify, and it decides the review interval and nothing
 * else --- not XP, not the review count, not whether the mistake is resolved. Those are derived, so
 * there is nowhere in this record for a client to put them.
 *
 * <p>The distinction matters most for the award: the XP is keyed by mistake and review number, so
 * replaying this request moves nothing twice. That is what makes a retried review safe after a
 * dropped connection, which is the same guarantee the completion endpoint gives.
 */
public record ReviewRequest(boolean correct) {
}
