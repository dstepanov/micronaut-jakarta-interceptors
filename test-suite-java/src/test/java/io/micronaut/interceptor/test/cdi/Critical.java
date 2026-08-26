package io.micronaut.interceptor.test.cdi;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding declared on another binding: an element declaring {@code @Critical} declares {@code @Monitored} too.
 */
@InterceptorBinding
@Monitored
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Critical {
}
