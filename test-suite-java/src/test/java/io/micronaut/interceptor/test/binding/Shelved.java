package io.micronaut.interceptor.test.binding;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding whose members default to empty values: an empty string, which Micronaut leaves out of the defaults it
 * records for an annotation, and an empty array, which it keeps.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Shelved {

    String value() default "";

    String[] labels() default {};
}
