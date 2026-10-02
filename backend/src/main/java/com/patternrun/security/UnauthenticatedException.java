package com.patternrun.security;

/** No usable session on an endpoint that needs one. Rendered as 401. */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("Sign in to continue.");
    }
}
