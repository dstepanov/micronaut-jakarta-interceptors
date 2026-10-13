package io.micronaut.interceptor.test.micronautapi;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding with a member that binds and a default for it, so that a method can declare the binding of its class
 * again and replace its value with the default, and a member that does not bind.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Banded {

    String value() default "default";

    @Nonbinding
    String note() default "";
}
