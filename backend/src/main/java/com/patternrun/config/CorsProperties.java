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

    /**
     * True when the web app is served from the same origin as this API.
     *
     * It decides how the session cookie is written. Same-origin is the deployment we want, and
     * it gets SameSite=Lax, which blocks cross-site writes for free. Development runs the
     * frontend on :3000 and the API on :8080, which browsers treat as cross-site, so there the
     * cookie has to be SameSite=None for the session to travel at all.
     *
     * Set by configuration rather than inferred from the origin list, because the origin list
     * can legitimately hold several production origins while the frontend is still same-origin
     * behind a rewrite.
     */
    private boolean sameOriginFrontend = false;

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

    public boolean isSameOriginFrontend() {
        return sameOriginFrontend;
    }

    public void setSameOriginFrontend(boolean sameOriginFrontend) {
        this.sameOriginFrontend = sameOriginFrontend;
    }
}
