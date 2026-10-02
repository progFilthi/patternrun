package com.patternrun.account.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Who the caller is. Deliberately thin: an anonymous learner has no email, no name and no
 * profile, which is what README section 89 asks for in anonymous mode.
 */
public record UserResponse(
        UUID id,
        String email,
        String username,
        boolean anonymous,
        String timezone,
        Instant createdAt) {
}
