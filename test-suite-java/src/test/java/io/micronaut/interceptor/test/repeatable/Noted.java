package io.micronaut.interceptor.test.repeatable;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A repeatable binding with a member excluded from the binding, which two occurrences may differ in. */
@InterceptorBinding
@Repeatable(Notes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Noted {

    String value();

    @Nonbinding
    String note() default "";
}
