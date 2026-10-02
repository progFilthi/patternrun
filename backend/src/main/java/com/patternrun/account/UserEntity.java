package com.patternrun.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A learner. Every visitor gets an anonymous row on their first request that needs identity
 * (README section 73), so progress is server-side from the very first session and registering
 * later claims the row rather than replacing it. Nothing about a session needs to move.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    /** Argon2id hash. Never a raw password (README section 89). */
    private String passwordHash;

    /** IANA zone, read once from the browser; streak days are computed in it server-side. */
    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(nullable = false)
    private boolean anonymous = true;

    /** Set when an anonymous row is claimed by a real account. */
    private Instant claimedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isRegistered() {
        return !anonymous;
    }
}
