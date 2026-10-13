package io.micronaut.interceptor.test.drift;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding with a member defaulting to an empty string, which Micronaut records no default for at compilation
 * time and the reflection module registers once it builds an instance of the annotation. Used by
 * {@link DefaultsDriftTest} alone, so that nothing else builds an instance of it first.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Drifting {

    String value() default "";

    String level() default "INFO";

    String[] tags() default {};

    Class<?> kind() default Object.class;

    RetentionPolicy policy() default RetentionPolicy.CLASS;

    int[] sizes() default {};
}
