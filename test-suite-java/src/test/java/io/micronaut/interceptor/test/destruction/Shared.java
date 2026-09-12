
package io.micronaut.interceptor.test.destruction;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the singleton interceptor, which no one object owns.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Shared {
}
