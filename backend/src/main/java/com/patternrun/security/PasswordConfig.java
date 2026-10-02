package com.patternrun.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing (README section 89: never store raw passwords).
 *
 * BCrypt rather than Argon2id. Argon2 is OWASP's first choice, but Spring Security's Argon2
 * encoder needs BouncyCastle on the classpath, and a self-hosted learning app is not worth a
 * cryptography dependency and its own CVE surface. BCrypt is memory-hard, pure Java, and needs
 * nothing added to the build. Cost 12 is deliberately slow: login happens a handful of times per
 * user, so the cost buys almost nothing to attack and a great deal of resistance if it does.
 *
 * The flip side of BCrypt is a hard 72-byte input limit, above which implementations silently
 * truncate. {@code RegisterRequest} caps the length so a long passphrase is rejected rather
 * than quietly weakened.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}