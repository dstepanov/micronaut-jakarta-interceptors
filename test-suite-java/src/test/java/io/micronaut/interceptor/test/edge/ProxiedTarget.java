package io.micronaut.interceptor.test.edge;

import io.micronaut.aop.Around;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Has Micronaut proxy a bean by wrapping a separate instance of it, rather than by making the proxy the bean.
 */
@Around(proxyTarget = true)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ProxiedTarget {
}
