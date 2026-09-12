package io.micronaut.interceptor.test.chaincache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that takes no part in the interception, and whose value is an evaluated expression.
 *
 * <p>Micronaut hands the advice the metadata of a method containing an expression wrapped around the evaluation
 * context of the invocation, which holds the object being invoked and the arguments it was invoked with, so that
 * the expression can read them. Every annotation read from that metadata comes back wrapped the same way,
 * {@code @JakartaInterception} included.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Noted {

    String value();
}
