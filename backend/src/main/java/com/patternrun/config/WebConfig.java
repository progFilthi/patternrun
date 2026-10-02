package com.patternrun.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * Origins come from configuration and never from "*", because the session cookie makes
     * every response credentialed (README section 72).
     *
     * Credentials are allowed so the browser will send the session cookie from a cross-origin
     * frontend. In production the Next.js rewrite makes both same-origin and the browser
     * treats this as a no-op; during development the frontend is on :3000 and the API on :8080,
     * which are genuinely cross-site, so the cookie has to travel cross-origin to work at all.
     *
     * That is also why `SessionCookie` switches to SameSite=None in development. Lax would be
     * better, but it would stop the browser sending the cookie cross-site and the app would
     * appear to lose every session on each request.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.resolveOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}