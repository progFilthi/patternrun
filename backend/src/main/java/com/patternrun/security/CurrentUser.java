package com.patternrun.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the caller's identity to a parameter.
 *
 * Required by default, because most endpoints that touch a learner should not silently serve an
 * anonymous caller. Use {@code required = false} for endpoints that are meaningful either way.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    boolean required() default true;
}
