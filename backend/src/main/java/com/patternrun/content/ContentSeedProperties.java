package com.patternrun.content;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Content seeding is owned by Spring, not by Flyway. Flyway owns the schema only
 * (README section 64); this content pass is idempotent and can be switched off.
 */
@ConfigurationProperties(prefix = "patternrun.seed")
public class ContentSeedProperties {

    /** Set to false to run against a database whose content is managed elsewhere. */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}