package io.micronaut.interceptor.test.graalvm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Returned by a member of {@link Produced}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Origin {
    String value();
}
