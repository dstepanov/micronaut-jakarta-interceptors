
package io.micronaut.interceptor.test.introduction;

import io.micronaut.aop.InterceptorBinding;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.aop.Introduction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Micronaut introduction advice: the methods of the interface are implemented by {@link Answering}.
 */
@Introduction
@InterceptorBinding(kind = InterceptorKind.INTRODUCTION)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Oracular {
}
