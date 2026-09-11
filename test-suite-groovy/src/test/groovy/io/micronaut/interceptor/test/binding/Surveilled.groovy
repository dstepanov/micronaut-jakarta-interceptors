package io.micronaut.interceptor.test.binding

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Not a binding annotation itself: it only carries {@link Watched}.
 */
@Watched
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface Surveilled {
}
