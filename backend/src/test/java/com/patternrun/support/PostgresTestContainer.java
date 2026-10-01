package com.patternrun.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One PostgreSQL container for the whole test run. Started eagerly so every integration test
 * class shares the same Spring context instead of creating a new container per class.
 */
public final class PostgresTestContainer {

    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:18-alpine");

    static {
        INSTANCE.start();
    }

    private PostgresTestContainer() {
    }
}