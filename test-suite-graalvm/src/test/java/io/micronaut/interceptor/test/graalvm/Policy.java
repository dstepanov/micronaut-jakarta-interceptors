package io.micronaut.interceptor.test.graalvm;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding whose nonbinding members return annotations: one set explicitly, one left to its default, and an array.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Policy {
    @Nonbinding Detail detail() default @Detail("default");

    @Nonbinding Detail fallback() default @Detail("fallback");

    @Nonbinding Detail[] details() default {};
}
