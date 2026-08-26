package io.micronaut.interceptor.test.binding;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Cached {

    /**
     * A binding member: an interceptor only intercepts the elements that declare the same region.
     */
    String region() default "default";

    /**
     * A member excluded from the binding, which two declarations may therefore differ in.
     */
    @Nonbinding
    String comment() default "";
}
