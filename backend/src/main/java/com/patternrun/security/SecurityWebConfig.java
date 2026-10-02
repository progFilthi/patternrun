package com.patternrun.security;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link CurrentUser} argument resolver.
 *
 * CORS stays where it already is, in {@code com.patternrun.config.WebConfig}, which owns the
 * origin list for the whole API. Keeping one place responsible for CORS means the account
 * endpoints cannot end up with different origin rules than the content endpoints.
 */
@Configuration
public class SecurityWebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public SecurityWebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}