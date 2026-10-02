package com.patternrun.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registering turns an anonymous row into an account. The row is claimed rather than replaced,
 * so nothing about the learner's history has to move.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt silently truncates past 72 bytes, so a longer passphrase is rejected rather
        // than quietly weakened. 8 is the floor, not a recommendation.
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_]+", message = "may only contain letters, numbers and underscores")
        String username,
        @Size(max = 64) String timezone) {
}
