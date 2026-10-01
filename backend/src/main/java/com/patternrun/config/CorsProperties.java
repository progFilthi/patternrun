package com.patternrun.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "patternrun.cors")
public class CorsProperties {

    /** Comma separated list of allowed origins. Never "*" in production (README section 72). */
    private String allowedOrigins = "http://localhost:3000";

    public List<String> resolveOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }
}