package io.micronaut.interceptor.test.repeatable;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A repeatable binding: an element may be bound by several occurrences of it, each carrying its own value. */
@InterceptorBinding
@Repeatable(Tags.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Tag {

    String value();
}
