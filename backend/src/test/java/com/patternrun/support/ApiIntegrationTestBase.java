package com.patternrun.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for API integration tests. Runs the real Flyway migrations against a real
 * PostgreSQL container, so the tests also prove that the schema matches the content
 * (README section 64).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiIntegrationTestBase {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = PostgresTestContainer.INSTANCE;

    @Autowired
    protected MockMvc mockMvc;
}