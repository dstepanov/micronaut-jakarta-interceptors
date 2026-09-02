package io.micronaut.interceptor.test.binding;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding of two members whose values are strings, which is what lets a declaration of it carry the punctuation
 * that separates one member from the next wherever a binding is written out as one string.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Tagged {

    String first() default "";

    String second() default "";
}
