package io.micronaut.interceptor.test.destruction;

import io.micronaut.aop.InterceptorBinding;
import io.micronaut.aop.InterceptorKind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Micronaut's own pre-destroy advice, which {@link SuppressedDestroyInterceptor} interposes with. */
@InterceptorBinding(kind = InterceptorKind.PRE_DESTROY)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SuppressedDestroyAdvice {
}
