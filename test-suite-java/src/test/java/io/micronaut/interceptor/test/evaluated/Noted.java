package io.micronaut.interceptor.test.evaluated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that takes no part in the interception, and whose value is an evaluated expression. It is what makes
 * Micronaut wrap the metadata of the method it is declared on.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Noted {

    String value();
}
