package io.micronaut.interceptor.test.introduction;

import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Micronaut introduction advice, which implements the abstract methods of the type it is declared on.
 */
@Introduction
@Bean
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Stubbed {
}
