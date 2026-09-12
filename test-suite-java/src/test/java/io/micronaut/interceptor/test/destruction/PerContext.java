package io.micronaut.interceptor.test.destruction;

import io.micronaut.runtime.context.scope.ScopedProxy;
import jakarta.inject.Scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A scope of the application's own, held by {@link PerContextScope}, whose beans Micronaut resolves through a proxy
 * over one target the scope keeps.
 */
@Scope
@ScopedProxy
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PerContext {
}
