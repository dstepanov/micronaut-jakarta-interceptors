package io.micronaut.interceptor.test.graalvm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Not a binding, but returned by members of one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Detail {
    String value();

    Note note() default @Note("none");
}
