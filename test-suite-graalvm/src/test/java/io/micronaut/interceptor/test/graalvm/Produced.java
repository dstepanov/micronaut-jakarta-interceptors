package io.micronaut.interceptor.test.graalvm;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding declared on a member of a factory and nowhere else, so that the factory is the only place its instances
 * are permitted from.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Produced {
    @Nonbinding Origin origin() default @Origin("unknown");
}
