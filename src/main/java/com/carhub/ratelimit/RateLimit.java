package com.carhub.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies a named rate-limit policy to a controller handler. The policy name is
 * resolved against {@code carhub.rate-limit.policies.<name>} at runtime, so the
 * actual limit/window stay in configuration rather than hardcoded here.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String policy();
}
