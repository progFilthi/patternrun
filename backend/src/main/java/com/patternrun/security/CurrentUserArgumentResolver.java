package com.patternrun.security;

import com.patternrun.account.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Injects the caller into a controller method.
 *
 * Declaring {@code @CurrentUser UserEntity user} means "this endpoint needs a learner", and
 * {@code @CurrentUser(required = false) UserEntity user} means "tell me who this is if anyone".
 * The distinction is the annotation's rather than a null check at each call site, so an endpoint
 * that forgets to handle an anonymous caller fails as a compile-time-shaped omission rather
 * than a null dereference.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserEntity.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer container,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        UserEntity user = request == null ? null : SessionAuthenticationFilter.currentUser(request);
        if (user == null && parameter.getParameterAnnotation(CurrentUser.class).required()) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    public static UUID idOf(UserEntity user) {
        return user == null ? null : user.getId();
    }
}