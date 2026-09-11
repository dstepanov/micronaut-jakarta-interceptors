package io.micronaut.interceptor.test.binding;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A binding annotation that carries {@link Watched}, as the specification lets one binding declare another. */
@InterceptorBinding
@Watched
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Guarded {
}
