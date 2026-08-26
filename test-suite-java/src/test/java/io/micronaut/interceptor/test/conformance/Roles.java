package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding annotation with an array-valued member. The specification leaves those to an extension; comparing them
 * by their contents is the natural reading, and is what this module does.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Roles {

    String[] value() default {};
}
