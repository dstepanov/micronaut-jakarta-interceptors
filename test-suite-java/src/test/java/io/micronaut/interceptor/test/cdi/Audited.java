package io.micronaut.interceptor.test.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A stereotype: not a binding of its own, but carrying one. The specification has a class annotated with it bound
 * by the binding it carries, and an {@code @Inherited} stereotype reach a subclass with its bindings.
 */
@Secure
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Audited {
}
