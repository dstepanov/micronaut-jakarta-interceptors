package io.micronaut.interceptor.test.graalvm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Returned by a member of {@link Detail}, two annotations deep below the binding.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Note {
    String value();
}
