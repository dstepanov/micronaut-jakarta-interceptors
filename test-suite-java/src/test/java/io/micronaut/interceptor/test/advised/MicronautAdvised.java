package io.micronaut.interceptor.test.advised;

import io.micronaut.aop.Around;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Micronaut advice of its own, which has Micronaut generate a subclass of the interceptor class declaring it.
 */
@Around
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MicronautAdvised {
}
